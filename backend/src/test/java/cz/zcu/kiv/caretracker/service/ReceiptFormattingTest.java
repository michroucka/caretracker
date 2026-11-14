package cz.zcu.kiv.caretracker.service;

import cz.zcu.kiv.caretracker.entity.Organization;
import cz.zcu.kiv.caretracker.entity.PerformedTask;
import cz.zcu.kiv.caretracker.entity.StatutoryHourlyLimit;
import cz.zcu.kiv.caretracker.entity.StatutoryTask;
import cz.zcu.kiv.caretracker.entity.Task;
import cz.zcu.kiv.caretracker.enums.UnitType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReceiptFormattingTest {

    private static final String NBSP = " ";

    private static Task task(long id, String name, UnitType unitType, int unitPrice) {
        Task task = new Task();
        task.setId(id);
        task.setName(name);
        task.setUnitType(unitType);
        task.setUnitPrice(unitPrice);
        return task;
    }

    private static PerformedTask record(Task task, double unitCount) {
        PerformedTask performedTask = new PerformedTask();
        performedTask.assignTask(task);
        performedTask.setUnitCount(unitCount);
        return performedTask;
    }

    private static List<ReceiptFormatting.BillingLine> lines(PerformedTask... records) {
        return ReceiptFormatting.billingLines(List.of(records), null);
    }

    private static final StatutoryHourlyLimit LIMIT = limit(80, 145);

    private static StatutoryHourlyLimit limit(int monthlyHoursThreshold, int maxUnitPriceOverThreshold) {
        StatutoryHourlyLimit limit = new StatutoryHourlyLimit();
        limit.setMonthlyHoursThreshold(monthlyHoursThreshold);
        limit.setMaxUnitPriceOverThreshold(maxUnitPriceOverThreshold);
        return limit;
    }

    private static Task statutoryTask(long id, String name, UnitType unitType, int unitPrice) {
        Task task = task(id, name, unitType, unitPrice);
        task.setStatutoryTask(new StatutoryTask());
        return task;
    }

    private static PerformedTask recordOn(Task task, double unitCount, LocalDateTime date, long id) {
        PerformedTask performedTask = record(task, unitCount);
        performedTask.setDate(date);
        performedTask.setId(id);
        return performedTask;
    }

    private static final LocalDateTime NOVEMBER = LocalDateTime.of(2026, 11, 2, 9, 0);

    @Test
    void hoursOverMonthlyThresholdAreBilledAtReducedRate() {
        Task dressing = statutoryTask(1, "Oblékání", UnitType.HOUR, 165);
        PerformedTask first = recordOn(dressing, 79 * 60, NOVEMBER, 1);
        PerformedTask second = recordOn(dressing, 2 * 60, NOVEMBER.plusDays(1), 2);

        List<ReceiptFormatting.BillingLine> lines = ReceiptFormatting.billingLines(List.of(first, second), LIMIT);

        assertEquals(List.of(79.0 * 60, 60.0, 60.0), lines.stream().map(ReceiptFormatting.BillingLine::unitCount).toList());
        assertEquals(List.of(165, 165, 145), lines.stream().map(ReceiptFormatting.BillingLine::unitPrice).toList());
        assertEquals(80 * 165 + 145, ReceiptFormatting.totalPrice(lines));

        List<Map<String, String>> rows = ReceiptFormatting.groupByTask(lines);
        assertEquals(List.of("165" + NBSP + "Kč / h", "145" + NBSP + "Kč / h"), rows.stream().map(row -> row.get("rate")).toList());
        assertEquals(List.of("80 h", "1 h"), rows.stream().map(row -> row.get("amount")).toList());
    }

    @Test
    void priceBelowReducedRateIsNotSplit() {
        Task dressing = statutoryTask(1, "Oblékání", UnitType.HOUR, 140);

        List<ReceiptFormatting.BillingLine> lines = ReceiptFormatting.billingLines(
                List.of(recordOn(dressing, 100 * 60, NOVEMBER, 1)), LIMIT);

        assertEquals(1, lines.size());
        assertEquals(100 * 140, ReceiptFormatting.totalPrice(lines));
    }

    @Test
    void ownHourlyTasksNeitherCountTowardsThresholdNorAreReduced() {
        Task ownTask = task(1, "Masáž", UnitType.HOUR, 300);
        Task dressing = statutoryTask(2, "Oblékání", UnitType.HOUR, 165);

        List<ReceiptFormatting.BillingLine> lines = ReceiptFormatting.billingLines(List.of(
                recordOn(ownTask, 90 * 60, NOVEMBER, 1),
                recordOn(dressing, 10 * 60, NOVEMBER.plusDays(1), 2)), LIMIT);

        assertEquals(90 * 300 + 10 * 165, ReceiptFormatting.totalPrice(lines));
    }

    @Test
    void thresholdFollowsRecordDateNotInsertionOrder() {
        Task dressing = statutoryTask(1, "Oblékání", UnitType.HOUR, 165);
        PerformedTask later = recordOn(dressing, 80 * 60, NOVEMBER.plusDays(5), 1);
        PerformedTask insertedLaterButEarlier = recordOn(dressing, 60, NOVEMBER, 2);

        List<ReceiptFormatting.BillingLine> lines = ReceiptFormatting.billingLines(List.of(later, insertedLaterButEarlier), LIMIT);

        assertEquals(List.of(insertedLaterButEarlier, later, later), lines.stream().map(ReceiptFormatting.BillingLine::record).toList());
        assertEquals(List.of(165, 165, 145), lines.stream().map(ReceiptFormatting.BillingLine::unitPrice).toList());
        assertEquals(List.of(60.0, 79.0 * 60, 60.0), lines.stream().map(ReceiptFormatting.BillingLine::unitCount).toList());
    }

    @Test
    void occurrenceAndWeightTasksDoNotCountAsHours() {
        Task dressing = statutoryTask(1, "Oblékání", UnitType.HOUR, 165);
        Task lunch = statutoryTask(2, "Oběd", UnitType.OCCURRENCE, 135);
        Task laundry = statutoryTask(3, "Praní", UnitType.KG, 110);

        List<ReceiptFormatting.BillingLine> lines = ReceiptFormatting.billingLines(List.of(
                recordOn(lunch, 100, NOVEMBER, 1),
                recordOn(laundry, 100, NOVEMBER, 2),
                recordOn(dressing, 80 * 60, NOVEMBER.plusDays(1), 3)), LIMIT);

        assertEquals(100 * 135 + 100 * 110 + 80 * 165, ReceiptFormatting.totalPrice(lines));
    }

    @Test
    void splitPartsAreRoundedSeparatelyLikeRecords() {
        Task dressing = statutoryTask(1, "Oblékání", UnitType.HOUR, 165);
        // 4799 min (13197.25 → 13197) fill the threshold up to 1 minute, the 11-minute record is 1 min at 165 (2.75 → 3) and 10 min at 145 (24.17 → 24)
        PerformedTask filler = recordOn(dressing, 4799, NOVEMBER, 1);
        PerformedTask crossing = recordOn(dressing, 11, NOVEMBER.plusDays(1), 2);

        List<ReceiptFormatting.BillingLine> lines = ReceiptFormatting.billingLines(List.of(filler, crossing), LIMIT);

        assertEquals(List.of(13197, 3, 24), lines.stream().map(ReceiptFormatting.BillingLine::price).toList());
    }

    @Test
    void withoutLimitNothingIsReduced() {
        Task dressing = statutoryTask(1, "Oblékání", UnitType.HOUR, 165);

        assertEquals(100 * 165, ReceiptFormatting.totalPrice(
                ReceiptFormatting.billingLines(List.of(recordOn(dressing, 100 * 60, NOVEMBER, 1)), null)));
    }

    @Test
    void totalPriceSumsRoundedPricesOfRecords() {
        Task hourly = task(1, "Doprovod", UnitType.HOUR, 100);
        // 10 min = 16.67 → 17 for each record, rounding is per record and not of the sum (33.33 → 33)
        assertEquals(34, ReceiptFormatting.totalPrice(lines(record(hourly, 10), record(hourly, 10))));
    }

    @Test
    void totalPriceOfNoRecordsIsZero() {
        assertEquals(0, ReceiptFormatting.totalPrice(lines()));
    }

    @ParameterizedTest(name = "{0}/{1}, klient {2} → {3}")
    @CsvSource({
            "11, 2026, 000123, 2611000123",
            "1,  2027, 010007, 2701010007",
            "12, 2000, 999999, 0012999999",
            "3,  2026, 010042, 2603010042",
    })
    void variableSymbolIsYearMonthAndPersonalNumber(int month, int year, String personalNumber, String expected) {
        assertEquals(expected, ReceiptFormatting.variableSymbol(personalNumber, month, year));
    }

    @Test
    void groupByTaskMakesOneRowPerTaskInOrderOfFirstAppearance() {
        Task walk = task(1, "Doprovod", UnitType.HOUR, 120);
        Task shopping = task(2, "Nákup", UnitType.OCCURRENCE, 50);

        List<Map<String, String>> rows = ReceiptFormatting.groupByTask(lines(
                record(walk, 45), record(shopping, 1), record(walk, 60), record(shopping, 2)));

        assertEquals(2, rows.size());
        assertEquals(Map.of(
                "taskName", "Doprovod",
                "recordCount", "2",
                "amount", "1 h 45 min",
                "rate", "120" + NBSP + "Kč / h",
                "price", "210" + NBSP + "Kč"), rows.get(0));
        assertEquals(Map.of(
                "taskName", "Nákup",
                "recordCount", "2",
                "amount", "3×",
                "rate", "50" + NBSP + "Kč / úkon",
                "price", "150" + NBSP + "Kč"), rows.get(1));
    }

    @Test
    void priceChangeWithinTheMonthGivesTheTaskASecondRow() {
        Task lunch = task(1, "Oběd", UnitType.OCCURRENCE, 80);
        PerformedTask before = record(lunch, 1);
        lunch.setUnitPrice(100);
        PerformedTask after = record(lunch, 2);

        List<Map<String, String>> rows = ReceiptFormatting.groupByTask(lines(before, after));

        assertEquals(List.of("80" + NBSP + "Kč / úkon", "100" + NBSP + "Kč / úkon"),
                rows.stream().map(row -> row.get("rate")).toList());
        assertEquals(List.of("80" + NBSP + "Kč", "200" + NBSP + "Kč"),
                rows.stream().map(row -> row.get("price")).toList());
    }

    @Test
    void groupedPriceIsSumOfRecordPrices() {
        Task hourly = task(1, "Doprovod", UnitType.HOUR, 100);

        List<Map<String, String>> rows = ReceiptFormatting.groupByTask(lines(record(hourly, 10), record(hourly, 10)));

        // same total as the receipt sum, not the price of 20 minutes (33)
        assertEquals("34" + NBSP + "Kč", rows.get(0).get("price"));
    }

    @ParameterizedTest(name = "{0} {1} → \"{2}\"")
    @CsvSource({
            "HOUR,       0,      0 min",
            "HOUR,       45,     45 min",
            "HOUR,       60,     1 h",
            "HOUR,       105,    1 h 45 min",
            "HOUR,       600,    10 h",
            "HOUR,       7.5,    8 min",
            "OCCURRENCE, 5,      5×",
            "OCCURRENCE, 1.5,    '1,5×'",
            "KG,         3.5,    '3,5 kg'",
            "KG,         2,      2 kg",
            "KG,         1.234,  '1,23 kg'",
            "KM,         12.75,  '12,75 km'",
    })
    void formatAmountByUnit(UnitType unitType, double unitCount, String expected) {
        assertEquals(expected, ReceiptFormatting.formatAmount(unitCount, unitType));
    }

    @Test
    void formatAmountSeparatesThousandsWithNonBreakingSpace() {
        assertEquals("1" + NBSP + "250 km", ReceiptFormatting.formatAmount(1250, UnitType.KM));
    }

    @ParameterizedTest(name = "{0} → \"{1}\"")
    @CsvSource({
            "HOUR,       h",
            "OCCURRENCE, úkon",
            "KG,         kg",
            "KM,         km",
    })
    void formatRateShowsPricePerUnit(UnitType unitType, String unit) {
        assertEquals("80" + NBSP + "Kč / " + unit, ReceiptFormatting.formatRate(unitType, 80));
    }

    @ParameterizedTest(name = "{0} → \"{1}\"")
    @CsvSource({
            "0,       0",
            "999,     999",
            "3081,    3|081",
            "1250000, 1|250|000",
    })
    void formatCurrencyUsesNonBreakingSpaces(int amount, String expectedDigits) {
        assertEquals(expectedDigits.replace("|", NBSP) + NBSP + "Kč", ReceiptFormatting.formatCurrency(amount));
    }

    @Test
    void formatAccountNumberWithPrefix() {
        assertEquals("19-123456789/0100", ReceiptFormatting.formatAccountNumber(organization("19", "123456789", "0100")));
    }

    @Test
    void formatAccountNumberWithoutPrefix() {
        assertEquals("123456789/0800", ReceiptFormatting.formatAccountNumber(organization(null, "123456789", "0800")));
    }

    @Test
    void formatAccountNumberIsNullWithoutAccountOrBankCode() {
        assertNull(ReceiptFormatting.formatAccountNumber(organization(null, null, "0800")));
        assertNull(ReceiptFormatting.formatAccountNumber(organization("19", "123456789", null)));
    }

    private static Organization organization(String prefix, String number, String bankCode) {
        Organization organization = new Organization();
        organization.setAccountPrefix(prefix);
        organization.setAccountNumber(number);
        organization.setBankCode(bankCode);
        return organization;
    }
}
