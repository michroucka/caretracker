import { useCallback, useEffect, useState } from "react";
import { getJSON } from "../api/api.js";
import { showErrorToast } from "../utils/errorHandler.jsx";

const NO_ITEMS = [];

/** Items of a collection nested under a department (`municipalities`, `supported-housings`); `reload` fetches them again. */
export function useDepartmentCollection(departmentId, collection, errorTitle) {
    const [loaded, setLoaded] = useState({ departmentId: null, items: NO_ITEMS });
    const [reloadCount, setReloadCount] = useState(0);

    useEffect(() => {
        if (!departmentId) return;
        let isOutdated = false;

        getJSON(`/departments/${departmentId}/${collection}`)
            .then((items) => { if (!isOutdated) setLoaded({ departmentId, items }); })
            .catch((error) => { if (!isOutdated) showErrorToast(error, errorTitle); });

        return () => { isOutdated = true; };
    }, [departmentId, collection, errorTitle, reloadCount]);

    const reload = useCallback(() => setReloadCount((count) => count + 1), []);

    // items of the previously chosen department must not be offered for the new one
    return { items: loaded.departmentId === departmentId ? loaded.items : NO_ITEMS, reload };
}
