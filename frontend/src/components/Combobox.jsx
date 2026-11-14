import React from "react";
import { IconCheck, IconSelector } from "@tabler/icons-react";
import { Button } from "@/components/ui/button";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { removeDiacritics } from "@/utils/formatters";
import { cn } from "@/lib/utils";

const NONE = "__none__";
const CUSTOM = "__custom__";

function filterOptions(value, search, keywords = []) {
    return removeDiacritics(keywords.join(" ")).includes(removeDiacritics(search.trim())) ? 1 : 0;
}

/**
 * Searchable single select.
 * options: [{ value, label, description? }]; `clearLabel` adds an option that sets the value to null.
 * `allowsCustomValue` also offers the typed text as a value, for fields where the options are only suggestions.
 * Extra props (id, aria-*) go to the trigger, so it works with FormField's controlProps.
 */
export function Combobox({ options, value, onChange, placeholder = "Vyberte…", searchPlaceholder = "Hledat…", emptyText = "Nic nenalezeno.", clearLabel, allowsCustomValue = false, disabled, className, ...triggerProps }) {
    const [isOpen, setIsOpen] = React.useState(false);
    const [search, setSearch] = React.useState("");
    const selected = options.find((option) => option.value === value);
    const triggerText = selected ? selected.label : allowsCustomValue && value ? value : null;
    const customValue = search.trim();
    const offersCustomValue = allowsCustomValue && customValue && !options.some((option) => removeDiacritics(option.label.toLowerCase()) === removeDiacritics(customValue.toLowerCase()));

    const select = (nextValue) => {
        onChange(nextValue === NONE ? null : nextValue);
        setIsOpen(false);
        setSearch("");
    };

    return (
        <Popover open={isOpen} onOpenChange={setIsOpen}>
            <PopoverTrigger asChild>
                <Button
                    {...triggerProps}
                    type="button"
                    variant="outline"
                    role="combobox"
                    aria-expanded={isOpen}
                    disabled={disabled}
                    className={cn("w-full min-w-0 justify-between bg-card px-3 text-base font-normal active:scale-100", !triggerText && "text-muted-foreground", className)}
                >
                    <span className="truncate">{triggerText ?? placeholder}</span>
                    <IconSelector className="text-muted-foreground" />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="flex max-h-(--radix-popover-content-available-height) w-(--radix-popover-trigger-width) flex-col p-0" align="start">
                <Command filter={filterOptions}>
                    <CommandInput placeholder={searchPlaceholder} value={search} onValueChange={setSearch} />
                    <CommandList>
                        <CommandEmpty>{emptyText}</CommandEmpty>
                        <CommandGroup>
                            {clearLabel && (
                                <CommandItem value={NONE} keywords={[clearLabel]} onSelect={() => select(NONE)}>
                                    <IconCheck className={cn("size-4", value == null ? "opacity-100" : "opacity-0")} />
                                    <span className="text-muted-foreground">{clearLabel}</span>
                                </CommandItem>
                            )}
                            {offersCustomValue && (
                                // keyed by the text, because cmdk reads an item's keywords only when it mounts
                                <CommandItem key={customValue} value={CUSTOM} keywords={[customValue]} onSelect={() => select(customValue)}>
                                    <span className="flex-1 truncate">Použít „{customValue}“</span>
                                </CommandItem>
                            )}
                            {options.map((option) => (
                                <CommandItem key={option.value} value={String(option.value)} keywords={[option.label, option.description ?? ""]} onSelect={() => select(option.value)}>
                                    <IconCheck className={cn("size-4", option.value === value ? "opacity-100" : "opacity-0")} />
                                    <span className="flex-1 truncate">{option.label}</span>
                                    {option.description && <span className="text-xs text-muted-foreground">{option.description}</span>}
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}
