import { useCallback, useState } from "react";
import { deleteJSON, downloadFile, fetchFile, getJSON, uploadFile } from "../api/api.js";
import { showErrorToast } from "../utils/errorHandler.jsx";
import { toast } from "@/components/ui/sonner";

export function useClientDocuments(clientId) {
    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchDocuments = useCallback(async () => {
        try {
            setDocuments(await getJSON(`/clients/${clientId}/documents`));
        } catch (err) {
            console.error("Error fetching client documents:", err);
            showErrorToast(err, "Chyba při načítání dokumentů");
        } finally {
            setLoading(false);
        }
    }, [clientId]);

    const uploadDocument = async (file, type) => {
        try {
            await uploadFile(`/clients/${clientId}/documents`, file, { type });
            toast.success("Dokument nahrán");
            await fetchDocuments();
        } catch (err) {
            console.error("Error uploading client document:", err);
            showErrorToast(err, "Chyba při nahrávání dokumentu");
            throw err;
        }
    };

    const deleteDocument = async (documentId) => {
        try {
            await deleteJSON(`/clients/${clientId}/documents/${documentId}`);
            toast.success("Dokument smazán");
            await fetchDocuments();
        } catch (err) {
            console.error("Error deleting client document:", err);
            showErrorToast(err, "Chyba při mazání dokumentu");
            throw err;
        }
    };

    const downloadDocument = async (document) => {
        try {
            await downloadFile(`/clients/${clientId}/documents/${document.id}`, document.fileName);
        } catch (err) {
            console.error("Error downloading client document:", err);
            showErrorToast(err, "Chyba při stahování dokumentu");
        }
    };

    // the stored content type wins, so the browser renders the file instead of offering it for download
    const fetchDocumentFile = async (document) => {
        try {
            const blob = await fetchFile(`/clients/${clientId}/documents/${document.id}`);
            return new Blob([blob], { type: document.contentType });
        } catch (err) {
            console.error("Error loading client document:", err);
            showErrorToast(err, "Chyba při načítání dokumentu");
            throw err;
        }
    };

    return { documents, loading, fetchDocuments, uploadDocument, deleteDocument, downloadDocument, fetchDocumentFile };
}
