"""Generates portraits with FLUX.1 [schnell] from a JSON list of {"id", "prompt", "seed"}.

Never keeps two big models on the GPU at once, so it stays well inside a 16 GB card that also drives
the Windows desktop: the text encoders encode every prompt and are freed, the Q5 GGUF transformer turns
the embeddings into latents and is freed, and only then the VAE decodes the latents into images.
A per-process memory cap makes a shortage end in an ordinary CUDA error instead of spilling into
shared system memory, which froze the whole machine once.

Usage: python generate.py people.json out_dir [--size 768]
"""
import argparse
import gc
import json
from pathlib import Path

import torch
from diffusers import AutoencoderKL, FluxPipeline, FluxTransformer2DModel, GGUFQuantizationConfig
from diffusers.image_processor import VaeImageProcessor
from huggingface_hub import hf_hub_download
from PIL import Image

BASE_REPO = "black-forest-labs/FLUX.1-schnell"
GGUF_REPO = "city96/FLUX.1-schnell-gguf"
GGUF_FILE = "flux1-schnell-Q5_K_S.gguf"
GPU_MEMORY_FRACTION = 0.7
OUTPUT_SIZE = 512


def free_gpu():
    gc.collect()
    torch.cuda.empty_cache()


def encode_prompts(people):
    pipe = FluxPipeline.from_pretrained(BASE_REPO, transformer=None, vae=None, torch_dtype=torch.bfloat16).to("cuda")
    embeddings = {}
    with torch.no_grad():
        for person in people:
            prompt_embeds, pooled_embeds, _ = pipe.encode_prompt(
                prompt=person["prompt"], prompt_2=None, max_sequence_length=256)
            embeddings[person["id"]] = (prompt_embeds.cpu(), pooled_embeds.cpu())
    del pipe
    free_gpu()
    return embeddings


def generate_latents(people, embeddings, size):
    transformer = FluxTransformer2DModel.from_single_file(
        hf_hub_download(GGUF_REPO, GGUF_FILE),
        quantization_config=GGUFQuantizationConfig(compute_dtype=torch.bfloat16),
        config=BASE_REPO, subfolder="transformer", torch_dtype=torch.bfloat16)
    pipe = FluxPipeline.from_pretrained(
        BASE_REPO, transformer=transformer, vae=None, text_encoder=None, text_encoder_2=None,
        tokenizer=None, tokenizer_2=None, torch_dtype=torch.bfloat16).to("cuda")
    latents = {}
    with torch.no_grad():
        for person in people:
            prompt_embeds, pooled_embeds = embeddings[person["id"]]
            result = pipe(
                prompt_embeds=prompt_embeds.to("cuda"), pooled_prompt_embeds=pooled_embeds.to("cuda"),
                num_inference_steps=4, guidance_scale=0.0, height=size, width=size, output_type="latent",
                generator=torch.Generator("cuda").manual_seed(person["seed"]),
            ).images
            latents[person["id"]] = FluxPipeline._unpack_latents(result, size, size, pipe.vae_scale_factor).cpu()
            print("latent", person["id"], flush=True)
    del pipe, transformer
    free_gpu()
    return latents


def decode_images(latents, out_dir):
    vae = AutoencoderKL.from_pretrained(BASE_REPO, subfolder="vae", torch_dtype=torch.bfloat16).to("cuda")
    vae.enable_tiling()
    processor = VaeImageProcessor(vae_scale_factor=2 ** (len(vae.config.block_out_channels) - 1) * 2)
    out_dir.mkdir(parents=True, exist_ok=True)
    with torch.no_grad():
        for person_id, latent in latents.items():
            latent = latent.to("cuda") / vae.config.scaling_factor + vae.config.shift_factor
            image = processor.postprocess(vae.decode(latent).sample, output_type="pil")[0]
            target = out_dir / f"{person_id}.jpg"
            target.parent.mkdir(parents=True, exist_ok=True)
            image.resize((OUTPUT_SIZE, OUTPUT_SIZE), Image.LANCZOS).save(target, quality=85)
            print("saved", person_id, flush=True)
    del vae
    free_gpu()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("people")
    parser.add_argument("out_dir")
    parser.add_argument("--size", type=int, default=768)
    parser.add_argument("--limit", type=int, default=None)
    args = parser.parse_args()
    torch.cuda.set_per_process_memory_fraction(GPU_MEMORY_FRACTION)
    people = json.loads(Path(args.people).read_text(encoding="utf-8"))[:args.limit]
    embeddings = encode_prompts(people)
    latents = generate_latents(people, embeddings, args.size)
    decode_images(latents, Path(args.out_dir))


if __name__ == "__main__":
    main()
