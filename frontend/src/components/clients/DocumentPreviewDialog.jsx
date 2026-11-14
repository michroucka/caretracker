import React from "react";
import { IconDownload, IconLoader2 } from "@tabler/icons-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";

/** Shows a client document in the application: a PDF in the browser's own viewer, an image as it is. */
export function DocumentPreviewDialog({ document, description, onClose, loadFile, onDownload }) {
    const [fileUrl, setFileUrl] = React.useState(null);

    React.useEffect(() => {
        if (!document) return undefined;
        let url = null;
        let ignore = false;
        loadFile(document)
            .then((blob) => {
                if (ignore) return;
                url = URL.createObjectURL(blob);
                setFileUrl(url);
            })
            .catch(() => onClose());
        return () => {
            ignore = true;
            if (url) URL.revokeObjectURL(url);
            setFileUrl(null);
        };
        // a new preview starts only when another document is opened
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [document]);

    const previewLabel = `Náhled dokumentu ${document?.fileName}`;

    return (
        <Dialog open={document != null} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="flex h-[90svh] flex-col sm:max-w-4xl">
                <DialogHeader>
                    <DialogTitle className="truncate">{document?.fileName}</DialogTitle>
                    <DialogDescription>{description}</DialogDescription>
                </DialogHeader>
                <div className="grid min-h-0 flex-1 place-items-center overflow-hidden rounded-lg border bg-muted">
                    {!fileUrl && <IconLoader2 className="size-6 animate-spin text-muted-foreground" aria-label="Načítám dokument" />}
                    {fileUrl && document && (document.contentType === "application/pdf"
                        ? <iframe src={fileUrl} title={previewLabel} className="size-full" />
                        : <img src={fileUrl} alt={previewLabel} className="max-h-full max-w-full object-contain" />)}
                </div>
                <DialogFooter>
                    <Button variant="outline" onClick={() => onDownload(document)}><IconDownload /> Stáhnout</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
