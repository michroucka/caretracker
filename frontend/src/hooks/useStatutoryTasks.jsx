import { useState } from "react";
import { getJSON, postJSON } from "../api/api.js";
import { showErrorToast } from "../utils/errorHandler.jsx";
import { pluralize } from "@/components/dashboard/dashboardFormat";
import { toast } from "@/components/ui/sonner";

const organizationQuery = (organizationId) => (organizationId ? `?organizationId=${organizationId}` : "");

export function useStatutoryTasks() {
    const [statutoryTasks, setStatutoryTasks] = useState([]);

    const fetchStatutoryTasks = async (organizationId) => {
        try {
            setStatutoryTasks(await getJSON(`/statutory-tasks${organizationQuery(organizationId)}`));
        } catch (err) {
            console.error("Error fetching statutory tasks:", err);
            showErrorToast(err, "Chyba při načítání úkonů z vyhlášky");
        }
    };

    const importStatutoryTasks = async (organizationId, statutoryTaskIds) => {
        try {
            const created = await postJSON(`/tasks/import-statutory${organizationQuery(organizationId)}`, { statutoryTaskIds });
            toast.success(`Načteno ${created.length} ${pluralize(created.length, ["úkon", "úkony", "úkonů"])}`);
            return created;
        } catch (err) {
            console.error("Error importing statutory tasks:", err);
            showErrorToast(err, "Chyba při načítání úkonů z vyhlášky");
            throw err;
        }
    };

    return { statutoryTasks, fetchStatutoryTasks, importStatutoryTasks };
}
