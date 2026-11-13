package cz.zcu.kiv.caretracker.service;

import cz.zcu.kiv.caretracker.entity.Organization;
import cz.zcu.kiv.caretracker.entity.PerformedTask;
import cz.zcu.kiv.caretracker.entity.StatutoryHourlyLimit;
import cz.zcu.kiv.caretracker.entity.Task;
import cz.zcu.kiv.caretracker.enums.UnitType;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Amounts, totals and texts printed on a receipt, kept apart from PDF rendering so they can be tested directly. */
final class ReceiptFormatting {

    private static final Locale CZECH = Locale.forLanguageTag("cs-CZ");

    private ReceiptFormatting() {
    }

    /** Part of a performed task billed at one unit price; a record crossing the monthly threshold has two. */
    record BillingLine(PerformedTask record, double unitCount, int unitPrice) {
        int price() {
            return PerformedTask.price(unitCount, unitPrice, record.getTask().getUnitType());
        }
    }

    /**
     * Turns one client's records of one calendar month into billing lines. Hours of tasks linked to the statutory
     * catalog are counted in the order of the record dates; minutes beyond the monthly threshold are billed at
     * {@code min(unit price, reduced maximum)} (decree 505/2006 Sb., § 6 odst. 2 písm. a). Without a limit, or for
     * other tasks, every record is one line at its stored price.
     *
     * @param records the client's records of the month
     * @param limit the statutory hourly limit, or null for none
     * @return billing lines, in date order when a limit applies
     */
    static List<BillingLine> billingLines(List<PerformedTask> records, StatutoryHourlyLimit limit) {
        if (limit == null) {
            return records.stream().map(record -> new BillingLine(record, record.getUnitCount(), record.getUnitPrice())).toList();
        }
        double minutesLeftUnderThreshold = limit.getMonthlyHoursThreshold() * 60.0;
        int reducedPrice = limit.getMaxUnitPriceOverThreshold();
        List<BillingLine> lines = new ArrayList<>();
        List<PerformedTask> byDate = records.stream()
                .sorted(Comparator.comparing(PerformedTask::getDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(PerformedTask::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        for (PerformedTask record : byDate) {
            if (!countsTowardsThreshold(record)) {
                lines.add(new BillingLine(record, record.getUnitCount(), record.getUnitPrice()));
                continue;
            }
            double underThreshold = Math.min(record.getUnitCount(), minutesLeftUnderThreshold);
            double overThreshold = record.getUnitCount() - underThreshold;
            minutesLeftUnderThreshold -= underThreshold;
            if (overThreshold == 0 || record.getUnitPrice() <= reducedPrice) {
                lines.add(new BillingLine(record, record.getUnitCount(), record.getUnitPrice()));
                continue;
            }
            if (underThreshold > 0) {
                lines.add(new BillingLine(record, underThreshold, record.getUnitPrice()));
            }
            lines.add(new BillingLine(record, overThreshold, reducedPrice));
        }
        return lines;
    }

    private static boolean countsTowardsThreshold(PerformedTask record) {
        Task task = record.getTask();
        return task.getUnitType() == UnitType.HOUR && task.getStatutoryTask() != null;
    }

    static int totalPrice(List<BillingLine> lines) {
        return lines.stream().mapToInt(BillingLine::price).sum();
    }

    /** Year, month and client's six-digit personal number: November 2026 for client 010042 → "2611010042". */
    static String variableSymbol(String personalNumber, int month, int year) {
        return String.format("%02d%02d%s", year % 100, month, personalNumber);
    }

    /**
     * Groups billing lines by task type and unit price and aggregates unit counts and prices per group
     * for use as rows in the receipt template, in the order the groups first appear. A price change within
     * the month, or hours over the statutory monthly threshold, give the task a second row with the other rate.
     */
    static List<Map<String, String>> groupByTask(List<BillingLine> lines) {
        return lines.stream()
                .collect(Collectors.groupingBy(
                        line -> List.of(line.record().getTask().getId(), line.unitPrice()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .values().stream()
                .map(group -> {
                    BillingLine first = group.get(0);
                    Task task = first.record().getTask();
                    double totalUnitCount = group.stream()
                            .mapToDouble(BillingLine::unitCount).sum();

                    Map<String, String> row = new HashMap<>();
                    row.put("taskName", task.getName());
                    row.put("recordCount", String.valueOf(group.size()));
                    row.put("amount", formatAmount(totalUnitCount, task.getUnitType()));
                    row.put("rate", formatRate(task.getUnitType(), first.unitPrice()));
                    row.put("price", formatCurrency(totalPrice(group)));
                    return row;
                })
                .toList();
    }

    /** 105 min → "1 h 45 min", 5 occurrences → "5×", 3.5 kg → "3,5 kg" */
    static String formatAmount(double unitCount, UnitType unitType) {
        if (unitType == UnitType.HOUR) {
            long minutes = Math.round(unitCount);
            long hours = minutes / 60;
            long rest = minutes % 60;
            if (hours == 0) return rest + " min";
            return rest == 0 ? hours + " h" : hours + " h " + rest + " min";
        }
        if (unitType == UnitType.OCCURRENCE) {
            return formatNumber(unitCount) + "×";
        }
        return formatNumber(unitCount) + " " + unitType.getLabel();
    }

    /** Price per unit; hourly tasks are recorded in minutes but priced per hour. */
    static String formatRate(UnitType unitType, int unitPrice) {
        String unit = switch (unitType) {
            case HOUR -> "h";
            case OCCURRENCE -> "úkon";
            default -> unitType.getLabel();
        };
        return formatCurrency(unitPrice) + " / " + unit;
    }

    /** 3081 → "3 081 Kč" with non-breaking spaces, so the amount never wraps. */
    static String formatCurrency(int amount) {
        return formatNumber(amount) + " Kč";
    }

    static String formatNumber(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(CZECH);
        format.setMaximumFractionDigits(2);
        return format.format(value).replace(' ', ' ');
    }

    /** "19-123456789/0100" as written on Czech payment orders, or null without a bank account. */
    static String formatAccountNumber(Organization organization) {
        if (organization.getAccountNumber() == null || organization.getBankCode() == null) {
            return null;
        }
        String prefix = organization.getAccountPrefix() != null ? organization.getAccountPrefix() + "-" : "";
        return prefix + organization.getAccountNumber() + "/" + organization.getBankCode();
    }
}
