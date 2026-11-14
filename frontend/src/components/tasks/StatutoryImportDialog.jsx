import React from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { formatTaskPrice, unitTypeLabel } from "./taskForm";

export function StatutoryImportDialog({ open, onOpenChange, statutoryTasks, onImport }) {
    const [selectedIds, setSelectedIds] = React.useState([]);
    const [isImporting, setIsImporting] = React.useState(false);

    const [wasOpen, setWasOpen] = React.useState(open);
    if (open !== wasOpen) {
        setWasOpen(open);
        if (open) setSelectedIds([]);
    }

    const importable = statutoryTasks.filter((item) => item.importedTaskId == null);
    const isChecked = (item) => item.importedTaskId != null || selectedIds.includes(item.id);

    const toggle = (id, checked) => setSelectedIds((previous) => checked ? [...previous, id] : previous.filter((selected) => selected !== id));

    const confirm = async () => {
        setIsImporting(true);
        try {
            await onImport(selectedIds);
            onOpenChange(false);
        } catch {
            // the hook already shows an error toast
        } finally {
            setIsImporting(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={(next) => !isImporting && onOpenChange(next)}>
            <DialogContent className="sm:max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Načíst úkony z vyhlášky</DialogTitle>
                    <DialogDescription>Vyberte úkony podle vyhlášky č. 505/2006 Sb., které se přidají do katalogu.</DialogDescription>
                </DialogHeader>
                <div className="flex gap-2">
                    <Button type="button" variant="outline" size="sm" onClick={() => setSelectedIds(importable.map((item) => item.id))} disabled={isImporting}>Vybrat vše</Button>
                    <Button type="button" variant="outline" size="sm" onClick={() => setSelectedIds([])} disabled={isImporting}>Zrušit výběr</Button>
                </div>
                <ul className="grid max-h-[50vh] gap-1 overflow-y-auto">
                    {statutoryTasks.map((item) => (
                        <li key={item.id} className="flex items-start gap-3 rounded-md p-2">
                            <Checkbox
                                id={`statutory-${item.id}`}
                                aria-label={item.shortName}
                                checked={isChecked(item)}
                                disabled={item.importedTaskId != null || isImporting}
                                onCheckedChange={(checked) => toggle(item.id, checked === true)}
                                className="mt-0.5"
                            />
                            <label htmlFor={`statutory-${item.id}`} className="grid flex-1 gap-0.5">
                                <span className="font-medium">{item.shortName}</span>
                                <span className="text-sm text-muted-foreground">{item.name}</span>
                                <span className="text-sm text-muted-foreground tabular-nums">
                                    {unitTypeLabel(item.unitType)} · max. {formatTaskPrice({ unitPrice: item.maxUnitPrice, unitType: item.unitType })}
                                </span>
                            </label>
                            {item.importedTaskId != null && <span className="text-sm text-muted-foreground">Už v katalogu</span>}
                        </li>
                    ))}
                </ul>
                <DialogFooter>
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isImporting}>Zrušit</Button>
                    <Button type="button" onClick={confirm} disabled={isImporting || selectedIds.length === 0}>{isImporting ? "Načítám…" : "Načíst vybrané"}</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
