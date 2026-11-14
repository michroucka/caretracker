import React from "react";
import { IconDownload, IconTrash } from "@tabler/icons-react";
import { useClientDocuments } from "@/hooks/useClientDocuments";
import { documentTypeOptions } from "@/constants/clientConstants";
import { formatDate } from "@/utils/formatters";
import { ConfirmDialog } from "@/components/data/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
                <ul className="grid gap-3">
                    {documents.map((document) => (
                        <li key={document.id} className="flex items-center justify-between gap-3">
                            <div className="grid min-w-0">
                                <span className="truncate font-medium">{document.fileName}</span>
                                <span className="text-sm text-muted-foreground">
                                    {[typeName(document.type), formatDate(document.uploadedAt), document.uploadedByName].filter(Boolean).join(" · ")}
                                </span>
                            </div>
                            <div className="flex shrink-0 gap-1">
                                <Button variant="ghost" size="icon" aria-label={`Stáhnout dokument ${document.fileName}`} onClick={() => downloadDocument(document)}><IconDownload /></Button>
                                {canModify && <Button variant="ghost" size="icon" aria-label={`Smazat dokument ${document.fileName}`} onClick={() => setDocumentToDelete(document)}><IconTrash /></Button>}
                            </div>
                        </li>
                    ))}
                </ul>
            )}
            {canModify && (
                <div className="grid gap-2">
                    <Select value={type} onValueChange={setType}>
                        <SelectTrigger aria-label="Typ dokumentu" className="w-full"><SelectValue placeholder="Typ dokumentu" /></SelectTrigger>
                        <SelectContent>
                            {documentTypeOptions.map((option) => <SelectItem key={option.key} value={option.key}>{option.name}</SelectItem>)}
                        </SelectContent>
                    </Select>
                    <Input type="file" aria-label="Soubor dokumentu" accept={ALLOWED_CONTENT_TYPES.join(",")} disabled={!type} onChange={upload} />
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
