import { FormField } from "@/components/FormField";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { statutoryPrefill, UNIT_TYPES } from "./taskForm";

const NO_STATUTORY_TASK = "none";

export function TaskFields({ idPrefix, values, errors, onChange, disabled, shakeSignal, statutoryTasks = [] }) {
    const priceSuffix = UNIT_TYPES.find((type) => type.value === values.unitType)?.priceSuffix;

    const chooseStatutoryTask = (value) => {
        const chosen = statutoryTasks.find((item) => String(item.id) === value);
        Object.entries(statutoryPrefill(chosen)).forEach(([field, fieldValue]) => onChange(field, fieldValue));
    };

    return (
        <div className="grid gap-4">
            {statutoryTasks.length > 0 && (
                <FormField id={`${idPrefix}-statutory`} label="Položka vyhlášky">
                    {(controlProps) => (
                        <Select value={values.statutoryTaskId == null ? NO_STATUTORY_TASK : String(values.statutoryTaskId)} onValueChange={chooseStatutoryTask} disabled={disabled}>
                            <SelectTrigger {...controlProps} className="w-full"><SelectValue /></SelectTrigger>
                            <SelectContent>
                                <SelectItem value={NO_STATUTORY_TASK}>Bez vazby na vyhlášku</SelectItem>
                                {statutoryTasks.map((item) => <SelectItem key={item.id} value={String(item.id)}>{item.code} {item.shortName}</SelectItem>)}
                            </SelectContent>
                        </Select>
                    )}
                </FormField>
            )}
            <FormField id={`${idPrefix}-name`} label="Název" required error={errors.name} shakeSignal={shakeSignal}>
                {(controlProps) => <Input {...controlProps} autoFocus disabled={disabled} value={values.name} onChange={(event) => onChange("name", event.target.value)} />}
            </FormField>
            <div className="grid grid-cols-2 items-start gap-3">
                <FormField id={`${idPrefix}-unit`} label="Jednotka" required>
                    {(controlProps) => (
                        <Select value={values.unitType} onValueChange={(unitType) => onChange("unitType", unitType)} disabled={disabled || values.statutoryTaskId != null}>
                            <SelectTrigger {...controlProps} className="w-full"><SelectValue /></SelectTrigger>
                            <SelectContent>
                                {UNIT_TYPES.map((type) => <SelectItem key={type.value} value={type.value}>{type.label}</SelectItem>)}
                            </SelectContent>
                        </Select>
                    )}
                </FormField>
                <FormField id={`${idPrefix}-price`} label="Cena" required error={errors.unitPrice} shakeSignal={shakeSignal}>
                    {(controlProps) => (
                        <div className="relative">
                            <Input {...controlProps} inputMode="numeric" className="pr-20 tabular-nums" disabled={disabled} value={values.unitPrice}
                                onChange={(event) => onChange("unitPrice", event.target.value.replace(/\D/g, ""))} />
                            <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-sm text-muted-foreground">{priceSuffix}</span>
                        </div>
                    )}
                </FormField>
            </div>
            <div className="flex items-start gap-3">
                <Checkbox
                    id={`${idPrefix}-double`}
                    checked={values.doubleMeeting}
                    onCheckedChange={(checked) => onChange("doubleMeeting", checked === true)}
                    disabled={disabled}
                    className="mt-0.5"
                />
                <div className="grid gap-0.5">
                    <Label htmlFor={`${idPrefix}-double`}>Dvojité setkání</Label>
                    <p className="text-sm text-muted-foreground">Jeden zapsaný úkon se do počtu setkání s klientem započítá dvakrát.</p>
                </div>
            </div>
        </div>
    );
}
