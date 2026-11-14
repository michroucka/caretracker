import React from "react";
import { IconDownload, IconFileTypePdf, IconPhoto, IconTrash, IconUpload } from "@tabler/icons-react";
import { useClientDocuments } from "@/hooks/useClientDocuments";
import { documentTypeOptions } from "@/constants/clientConstants";
import { formatDate } from "@/utils/formatters";
import { ConfirmDialog } from "@/components/data/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { toast } from "@/components/ui/sonner";

const ALLOWED_CONTENT_TYPES = ["application/pdf", "image/jpeg", "image/png"];
const MAX_FILE_SIZE = 5 * 1024 * 1024;

const typeName = (type) => documentTypeOptions.find((option) => option.key === type)?.name ?? type;

function validateFile(file) {
    if (!ALLOWED_CONTENT_TYPES.includes(file.type)) return "Nahrát lze jen PDF, JPEG nebo PNG";
    if (file.size > MAX_FILE_SIZE) return "Soubor je příliš velký. Maximální velikost je 5 MB";
    return null;
}

/** Documents of a client. Without `canModify` (terminated client) they can only be downloaded. */
export function ClientDocuments({ clientId, canModify }) {
    const { documents, loading, fetchDocuments, uploadDocument, deleteDocument, downloadDocument } = useClientDocuments(clientId);
    const [type, setType] = React.useState("");
    const [documentToDelete, setDocumentToDelete] = React.useState(null);
    const fileInputRef = React.useRef(null);

    React.useEffect(() => {
        fetchDocuments();
    }, [fetchDocuments]);

    const upload = async (event) => {
        const file = event.target.files[0];
        event.target.value = "";
        if (!file) return;
        const problem = validateFile(file);
        if (problem) {
            toast.error(problem);
            return;
        }
        await uploadDocument(file, type).catch(() => {});
    };

    return (
        <div className="grid gap-4">
            {!loading && documents.length === 0 && <p className="text-muted-foreground">Klient zatím nemá žádné dokumenty.</p>}
            {documents.length > 0 && (
                <ul className="flex flex-wrap gap-2">
                    {documents.map((document) => (
                        <li key={document.id} className="flex max-w-full items-center gap-2 rounded-lg border py-1.5 pr-1 pl-2.5 sm:max-w-80">
                            {document.contentType === "application/pdf"
                                ? <IconFileTypePdf className="size-5 shrink-0 text-muted-foreground" aria-hidden="true" />
                                : <IconPhoto className="size-5 shrink-0 text-muted-foreground" aria-hidden="true" />}
                            <div className="grid min-w-0 flex-1">
                                <span className="truncate text-sm font-medium">{document.fileName}</span>
                                <span className="truncate text-xs text-muted-foreground">
                                    {[typeName(document.type), formatDate(document.uploadedAt), document.uploadedByName].filter(Boolean).join(" · ")}
                                </span>
                            </div>
                            <div className="flex shrink-0">
                                <Button variant="ghost" size="icon-row" aria-label={`Stáhnout dokument ${document.fileName}`} onClick={() => downloadDocument(document)}><IconDownload /></Button>
                                {canModify && <Button variant="ghost" size="icon-row" aria-label={`Smazat dokument ${document.fileName}`} onClick={() => setDocumentToDelete(document)}><IconTrash /></Button>}
                            </div>
                        </li>
                    ))}
                </ul>
            )}
            {canModify && (
                <div className="flex flex-wrap items-center gap-2">
                    <Select value={type} onValueChange={setType}>
                        <SelectTrigger aria-label="Typ dokumentu" className="w-48"><SelectValue placeholder="Typ dokumentu" /></SelectTrigger>
                        <SelectContent>
                            {documentTypeOptions.map((option) => <SelectItem key={option.key} value={option.key}>{option.name}</SelectItem>)}
                        </SelectContent>
                    </Select>
                    <input ref={fileInputRef} type="file" aria-label="Soubor dokumentu" accept={ALLOWED_CONTENT_TYPES.join(",")} className="sr-only" disabled={!type} onChange={upload} />
                    <Button type="button" variant="outline" disabled={!type} onClick={() => fileInputRef.current?.click()}><IconUpload /> Nahrát soubor</Button>
                </div>
            )}
            <ConfirmDialog
                open={documentToDelete != null}
                onOpenChange={(open) => !open && setDocumentToDelete(null)}
                title="Smazat dokument?"
                description={`Dokument ${documentToDelete?.fileName} bude trvale smazán.`}
                confirmLabel="Smazat"
                destructive
                onConfirm={() => deleteDocument(documentToDelete.id)}
            />
        </div>
    );
}
