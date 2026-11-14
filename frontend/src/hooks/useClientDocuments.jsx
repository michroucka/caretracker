import { useCallback, useState } from "react";
import { deleteJSON, downloadFile, getJSON, uploadFile } from "../api/api.js";
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

    return { documents, loading, fetchDocuments, uploadDocument, deleteDocument, downloadDocument };
}
