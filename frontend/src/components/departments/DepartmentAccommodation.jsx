import React from "react";
import { IconBan, IconPencil, IconPlus, IconX } from "@tabler/icons-react";
import { deleteJSON, postJSON, putJSON } from "@/api/api.js";
import { FormField } from "@/components/FormField";
import { ConfirmDialog } from "@/components/data/ConfirmDialog";
import { SheetSection } from "@/components/data/RecordSheet";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { toast } from "@/components/ui/sonner";
import { useDepartmentCollection } from "@/hooks/useDepartmentCollection";
import { useRecordForm } from "@/hooks/useRecordForm";
import { showErrorToast } from "@/utils/errorHandler.jsx";
import { formatPostalCode } from "@/utils/formatters";

const EMPTY_HOUSING = { name: "", street: "", city: "", postalCode: "" };

function validateHousing(values) {
    const errors = {};
    if (!values.name.trim()) errors.name = "Prosím zadejte název domu";
    if (!values.street.trim()) errors.street = "Prosím zadejte ulici a číslo popisné";
    if (!values.city.trim()) errors.city = "Prosím zadejte město";
    if (!values.postalCode.trim()) errors.postalCode = "Prosím zadejte PSČ";
    return errors;
}

function toHousingRequest(values) {
    return { name: values.name.trim(), street: values.street.trim(), city: values.city.trim(), postalCode: values.postalCode };
}

/** Runs a change, shows the server message when it fails and rethrows so that callers can keep their form or dialog open. */
async function change(request, errorTitle) {
    try {
        return await request();
    } catch (error) {
        showErrorToast(error, errorTitle);
        throw error;
    }
}

function MunicipalitiesSection({ departmentId, canManage }) {
    const { items: municipalities, reload } = useDepartmentCollection(departmentId, "municipalities", "Chyba při načítání obcí");
    const [name, setName] = React.useState("");
    const [error, setError] = React.useState(null);

    const add = async (event) => {
        event.preventDefault();
        if (!name.trim()) {
            setError("Zadejte název obce");
            return;
        }
        try {
            await change(() => postJSON(`/departments/${departmentId}/municipalities`, { name: name.trim() }), "Chyba při přidávání obce");
            setName("");
            reload();
        } catch {
            // the toast is shown, the typed name stays
        }
    };

    const remove = async (municipality) => {
        try {
            await change(() => deleteJSON(`/departments/${departmentId}/municipalities/${municipality.id}`), "Chyba při odebírání obce");
            reload();
        } catch {
            // the toast is shown
        }
    };

    return (
        <SheetSection label="Obce">
            {municipalities.length > 0 ? (
                <ul aria-label="Obce střediska" className="flex flex-wrap gap-1.5">
                    {municipalities.map((municipality) => (
                        <li key={municipality.id} className="inline-flex items-center gap-1 rounded-md bg-secondary py-0.5 pr-0.5 pl-2.5 text-sm text-secondary-foreground">
                            {municipality.name}
                            {canManage && (
                                <Button type="button" variant="ghost" size="icon-row" className="size-6" aria-label={`Odebrat obec ${municipality.name}`} onClick={() => remove(municipality)}>
                                    <IconX />
                                </Button>
                            )}
                        </li>
                    ))}
                </ul>
            ) : <p className="text-muted-foreground">Středisko zatím nemá žádné obce.</p>}
            {canManage && (
                <form noValidate onSubmit={add} className="flex items-end gap-2">
                    <FormField id="municipality-name" label="Název obce" error={error} className="flex-1">
                        {(controlProps) => <Input {...controlProps} autoComplete="off" value={name} onChange={(event) => { setName(event.target.value); setError(null); }} />}
                    </FormField>
                    <Button type="submit" variant="outline"><IconPlus /> Přidat obec</Button>
                </form>
            )}
        </SheetSection>
    );
}

function HousingForm({ housing, onSubmit, onCancel }) {
    const form = useRecordForm(housing ? { ...housing, postalCode: formatPostalCode(housing.postalCode) } : EMPTY_HOUSING);
    const update = (field) => (event) => form.setField(field, event.target.value);
    const field = (name, label) => ({ id: `housing-${name}`, label, required: true, error: form.errors[name], shakeSignal: form.submitCount });

    const save = (event) => {
        event.preventDefault();
        form.submit(validateHousing, (values) => onSubmit(toHousingRequest(values)));
    };

    return (
        <form noValidate onSubmit={save} className="grid gap-3 rounded-lg border p-3">
            <FormField {...field("name", "Název domu")}>
                {(controlProps) => <Input {...controlProps} autoComplete="off" disabled={form.isSubmitting} value={form.values.name} onChange={update("name")} />}
            </FormField>
            <FormField {...field("street", "Ulice a číslo popisné")}>
                {(controlProps) => <Input {...controlProps} autoComplete="off" disabled={form.isSubmitting} value={form.values.street} onChange={update("street")} />}
            </FormField>
            <div className="grid grid-cols-[minmax(0,2fr)_minmax(0,1fr)] items-start gap-3">
                <FormField {...field("city", "Město")}>
                    {(controlProps) => <Input {...controlProps} autoComplete="off" disabled={form.isSubmitting} value={form.values.city} onChange={update("city")} />}
                </FormField>
                <FormField {...field("postalCode", "PSČ")}>
                    {(controlProps) => <Input {...controlProps} inputMode="numeric" disabled={form.isSubmitting} value={form.values.postalCode} onChange={(event) => form.setField("postalCode", formatPostalCode(event.target.value))} />}
                </FormField>
            </div>
            <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={onCancel} disabled={form.isSubmitting}>Zrušit</Button>
                <Button type="submit" disabled={form.isSubmitting}>Uložit dům</Button>
            </div>
        </form>
    );
}

function SupportedHousingsSection({ departmentId, canManage }) {
    const { items: housings, reload } = useDepartmentCollection(departmentId, "supported-housings", "Chyba při načítání domů s pečovatelskou službou");
    // "new", a housing being edited, or null when no form is open
    const [editing, setEditing] = React.useState(null);
    const [terminating, setTerminating] = React.useState(null);
    const basePath = `/departments/${departmentId}/supported-housings`;

    const saveHousing = async (request) => {
        const isNew = editing === "new";
        await change(
            () => (isNew ? postJSON(basePath, request) : putJSON(`${basePath}/${editing.id}`, request)),
            isNew ? "Chyba při přidávání domu" : "Chyba při ukládání domu",
        );
        toast.success(isNew ? "Dům přidán" : "Dům uložen");
        setEditing(null);
        reload();
    };

    const terminate = async () => {
        await change(() => putJSON(`${basePath}/${terminating.id}/terminate`), "Chyba při ukončení domu");
        toast.success("Dům ukončen");
        reload();
    };

    return (
        <SheetSection label="Domy s pečovatelskou službou">
            {housings.length > 0 ? (
                <ul aria-label="Domy s pečovatelskou službou" className="grid gap-2">
                    {housings.map((housing) => (
                        <li key={housing.id} className="flex items-center justify-between gap-3">
                            <div className="min-w-0">
                                <p className="flex items-center gap-2 font-medium">
                                    <span className="truncate">{housing.name}</span>
                                    {!housing.active && <Badge variant="outline">Ukončený</Badge>}
                                </p>
                                <p className="text-sm text-muted-foreground">{housing.street}, {housing.postalCode} {housing.city}</p>
                            </div>
                            {canManage && (
                                <div className="flex shrink-0 gap-1">
                                    <Button type="button" variant="ghost" size="icon-row" aria-label={`Upravit dům ${housing.name}`} onClick={() => setEditing(housing)}><IconPencil /></Button>
                                    {housing.active && (
                                        <Button type="button" variant="ghost" size="icon-row" aria-label={`Ukončit dům ${housing.name}`} onClick={() => setTerminating(housing)}><IconBan /></Button>
                                    )}
                                </div>
                            )}
                        </li>
                    ))}
                </ul>
            ) : <p className="text-muted-foreground">Středisko zatím nemá žádné domy s pečovatelskou službou.</p>}
            {canManage && (editing ? (
                <HousingForm key={editing === "new" ? "new" : editing.id} housing={editing === "new" ? null : editing} onSubmit={saveHousing} onCancel={() => setEditing(null)} />
            ) : (
                <Button type="button" variant="outline" className="justify-self-start" onClick={() => setEditing("new")}><IconPlus /> Přidat dům</Button>
            ))}
            <ConfirmDialog
                open={terminating != null}
                onOpenChange={(open) => !open && setTerminating(null)}
                title="Ukončit dům?"
                description={`Dům ${terminating?.name} půjde dál vidět u klientů, kteří v něm bydlí, ale nepůjde ho vybrat novým klientům.`}
                confirmLabel="Ukončit"
                destructive
                onConfirm={terminate}
            />
        </SheetSection>
    );
}

export function DepartmentAccommodation({ departmentId, canManage }) {
    return (
        <>
            <Separator />
            <MunicipalitiesSection departmentId={departmentId} canManage={canManage} />
            <Separator />
            <SupportedHousingsSection departmentId={departmentId} canManage={canManage} />
        </>
    );
}
