package com.borisey.personal_finance.services;

import com.borisey.personal_finance.models.Transaction;
import com.borisey.personal_finance.models.User;
import com.borisey.personal_finance.repo.TransactionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.apache.poi.xddf.usermodel.chart.*;
import java.time.format.DateTimeFormatter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    private final TransactionRepository transactionRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Autowired
    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public byte[] generateTransactionsReport(User user, Specification<Transaction> spec,
                                             LocalDateTime startDate, LocalDateTime endDate) throws IOException {
        logger.info("Формирование Excel отчета по транзакциям для пользователя '{}'", user.getUsername());

        List<Transaction> transactions = transactionRepository.findAll(
                spec, Sort.by(Sort.Direction.DESC, "operationDateTime"));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Транзакции");

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Отчет по финансовым операциям");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));


            Row subTitleRow = sheet.createRow(1);
            Cell subTitleCell = subTitleRow.createCell(0);
            subTitleCell.setCellValue("Период: " +
                    startDate.format(DATE_FORMATTER) + " - " +
                    endDate.format(DATE_FORMATTER));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            Row headerRow = sheet.createRow(3);
            String[] headers = {"Дата и время", "Тип", "Категория", "Сумма", "Статус",
                    "Банк отправителя", "Банк получателя", "Комментарий"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 256 * 15);
            }

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle negativeStyle = workbook.createCellStyle();
            Font redFont = workbook.createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            negativeStyle.setFont(redFont);
            negativeStyle.setBorderBottom(BorderStyle.THIN);
            negativeStyle.setBorderTop(BorderStyle.THIN);
            negativeStyle.setBorderLeft(BorderStyle.THIN);
            negativeStyle.setBorderRight(BorderStyle.THIN);

            int rowNum = 4;
            for (Transaction transaction : transactions) {
                Row row = sheet.createRow(rowNum++);

                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(transaction.getOperationDateTime().format(DATE_FORMATTER));
                dateCell.setCellStyle(dataStyle);

                Cell typeCell = row.createCell(1);
                typeCell.setCellValue(transaction.getTransactionType().getTitle());
                typeCell.setCellStyle(dataStyle);

                Cell categoryCell = row.createCell(2);
                if (transaction.getCategory() != null) {
                    categoryCell.setCellValue(transaction.getCategory().getTitle());
                } else {
                    categoryCell.setCellValue("Без категории");
                }
                categoryCell.setCellStyle(dataStyle);

                Cell amountCell = row.createCell(3);
                amountCell.setCellValue(transaction.getAmount().doubleValue());
                if (transaction.getTransactionType().getCode().equals("EXPENSE") ||
                        transaction.getTransactionType().getCode().equals("TRANSFER")) {
                    amountCell.setCellStyle(negativeStyle);
                } else {
                    amountCell.setCellStyle(dataStyle);
                }

                Cell statusCell = row.createCell(4);
                statusCell.setCellValue(transaction.getStatus().getTitle());
                statusCell.setCellStyle(dataStyle);

                Cell senderBankCell = row.createCell(5);
                if (transaction.getSenderBank() != null) {
                    senderBankCell.setCellValue(transaction.getSenderBank().getTitle());
                }
                senderBankCell.setCellStyle(dataStyle);

                Cell recipientBankCell = row.createCell(6);
                if (transaction.getRecipientBank() != null) {
                    recipientBankCell.setCellValue(transaction.getRecipientBank().getTitle());
                }
                recipientBankCell.setCellStyle(dataStyle);

                Cell commentCell = row.createCell(7);
                commentCell.setCellValue(transaction.getComment());
                commentCell.setCellStyle(dataStyle);
            }

            Row totalRow = sheet.createRow(rowNum + 1);
            Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("ИТОГО:");

            CellStyle totalStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            totalLabelCell.setCellStyle(totalStyle);

            BigDecimal totalIncome = transactionRepository.sumAmountByTransactionType(
                    user.getId(), "INCOME", startDate, endDate);
            totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;

            List<String> expenseTypes = java.util.Arrays.asList("EXPENSE", "TRANSFER");
            BigDecimal totalExpense = transactionRepository.sumAmountByTransactionTypes(
                    user.getId(), expenseTypes, startDate, endDate);
            totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;

            Row incomesRow = sheet.createRow(rowNum + 2);
            incomesRow.createCell(0).setCellValue("Доходы:");
            Cell incomeCell = incomesRow.createCell(1);
            incomeCell.setCellValue(totalIncome.doubleValue());

            Row expensesRow = sheet.createRow(rowNum + 3);
            expensesRow.createCell(0).setCellValue("Расходы:");
            Cell expenseCell = expensesRow.createCell(1);
            expenseCell.setCellValue(totalExpense.doubleValue());
            expenseCell.setCellStyle(negativeStyle);

            Row balanceRow = sheet.createRow(rowNum + 4);
            balanceRow.createCell(0).setCellValue("Баланс:");
            Cell balanceCell = balanceRow.createCell(1);
            BigDecimal balance = totalIncome.subtract(totalExpense);
            balanceCell.setCellValue(balance.doubleValue());

            workbook.write(out);
            logger.info("Excel отчет успешно сформирован для пользователя '{}'", user.getUsername());
            return out.toByteArray();
        }
    }

    public byte[] generateCategoryReportByType(User user, String typeCode) throws IOException {
        logger.info("Формирование Excel отчета по категориям типа {} для пользователя '{}'",
                typeCode, user.getUsername());

        List<Object[]> categorySums = transactionRepository.sumAmountByCategory(user.getId(), typeCode);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Категории " +
                    (typeCode.equals("INCOME") ? "доходов" : "расходов"));

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Отчет по категориям " +
                    (typeCode.equals("INCOME") ? "доходов" : "расходов"));

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusMonths(1);

            Row subTitleRow = sheet.createRow(1);
            Cell subTitleCell = subTitleRow.createCell(0);
            subTitleCell.setCellValue("Период: " +
                    startDate.format(DATE_FORMATTER) + " - " +
                    endDate.format(DATE_FORMATTER));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

            Row headerRow = sheet.createRow(3);
            String[] headers = {"Категория", "Сумма", "% от общей суммы"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 256 * 20);
            }

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
            percentStyle.setBorderBottom(BorderStyle.THIN);
            percentStyle.setBorderTop(BorderStyle.THIN);
            percentStyle.setBorderLeft(BorderStyle.THIN);
            percentStyle.setBorderRight(BorderStyle.THIN);

            if (categorySums.isEmpty()) {
                Row noDataRow = sheet.createRow(4);
                Cell noDataCell = noDataRow.createCell(0);
                noDataCell.setCellValue("Нет данных за выбранный период");
                noDataCell.setCellStyle(dataStyle);
                sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 2));
            } else {
                BigDecimal totalSum = BigDecimal.ZERO;
                Map<String, BigDecimal> categoryAmounts = new HashMap<>();

                for (Object[] result : categorySums) {
                    String categoryTitle = (String) result[0];
                    BigDecimal amount = (BigDecimal) result[1];

                    if (categoryTitle != null && amount != null) {
                        categoryAmounts.put(categoryTitle, amount);
                        totalSum = totalSum.add(amount);
                    }
                }

                int rowNum = 4;
                for (Map.Entry<String, BigDecimal> entry : categoryAmounts.entrySet()) {
                    Row row = sheet.createRow(rowNum++);

                    Cell categoryCell = row.createCell(0);
                    categoryCell.setCellValue(entry.getKey());
                    categoryCell.setCellStyle(dataStyle);

                    Cell amountCell = row.createCell(1);
                    amountCell.setCellValue(entry.getValue().doubleValue());
                    amountCell.setCellStyle(dataStyle);
                    Cell percentCell = row.createCell(2);
                    double percentage = totalSum.signum() == 0 ? 0 :
                            entry.getValue().divide(totalSum, 4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    percentCell.setCellValue(percentage);
                    percentCell.setCellStyle(percentStyle);
                }

                Row totalRow = sheet.createRow(rowNum + 1);
                Cell totalLabelCell = totalRow.createCell(0);
                totalLabelCell.setCellValue("ИТОГО:");

                CellStyle totalStyle = workbook.createCellStyle();
                Font totalFont = workbook.createFont();
                totalFont.setBold(true);
                totalStyle.setFont(totalFont);
                totalLabelCell.setCellStyle(totalStyle);

                Cell totalAmountCell = totalRow.createCell(1);
                totalAmountCell.setCellValue(totalSum.doubleValue());
                totalAmountCell.setCellStyle(totalStyle);

                if (!categoryAmounts.isEmpty()) {
                    try {
                        rowNum += 3;

                        Row chartTitleRow = sheet.createRow(rowNum++);
                        Cell chartTitleCell = chartTitleRow.createCell(0);
                        chartTitleCell.setCellValue("Распределение по категориям");
                        chartTitleCell.setCellStyle(titleStyle);

                        XSSFSheet xssfSheet = (XSSFSheet) sheet;
                        XSSFDrawing drawing = xssfSheet.createDrawingPatriarch();

                        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, rowNum, 14, rowNum + 20);

                        XSSFChart chart = drawing.createChart(anchor);
                        chart.setTitleText("Распределение по категориям");
                        chart.setTitleOverlay(false);

                        XDDFChartLegend legend = chart.getOrAddLegend();
                        legend.setPosition(LegendPosition.RIGHT);

                        XDDFDataSource<String> cats = XDDFDataSourcesFactory.fromStringCellRange(
                                xssfSheet,
                                new CellRangeAddress(4, 3 + categoryAmounts.size(), 0, 0));

                        XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromNumericCellRange(
                                xssfSheet,
                                new CellRangeAddress(4, 3 + categoryAmounts.size(), 1, 1));

                        XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);
                        data.setVaryColors(true);
                        XDDFChartData.Series series = data.addSeries(cats, vals);
                        series.setTitle("Категории", null);

                        chart.plot(data);

                        if (chart.getCTChart().getPlotArea().getPieChartArray().length > 0) {
                            chart.getCTChart().getPlotArea().getPieChartArray()[0].addNewDLbls();
                            chart.getCTChart().getPlotArea().getPieChartArray()[0].getDLbls().addNewShowVal().setVal(false);
                            chart.getCTChart().getPlotArea().getPieChartArray()[0].getDLbls().addNewShowPercent().setVal(true);
                            chart.getCTChart().getPlotArea().getPieChartArray()[0].getDLbls().addNewShowCatName().setVal(false);
                        }
                    } catch (Exception e) {
                        logger.error("Ошибка при создании диаграммы: " + e.getMessage(), e);
                        Row errorRow = sheet.createRow(rowNum + 3);
                        Cell errorCell = errorRow.createCell(0);
                        errorCell.setCellValue("Не удалось создать диаграмму: " + e.getMessage());
                    }
                }
            }

            workbook.write(out);
            logger.info("Excel отчет по категориям типа {} успешно сформирован для пользователя '{}'",
                    typeCode, user.getUsername());
            return out.toByteArray();
        }
    }


    public byte[] generateDashboardReport(User user, LocalDateTime startDate, LocalDateTime endDate) throws IOException {
        logger.info("Формирование сводного Excel отчета для пользователя '{}' за период с {} по {}",
                user.getUsername(), startDate.format(DATE_FORMATTER), endDate.format(DATE_FORMATTER));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            LocalDateTime yearStart = endDate.minusYears(1);
            LocalDateTime quarterStart = endDate.minusMonths(3);
            LocalDateTime monthStart = endDate.minusMonths(1);
            LocalDateTime weekStart = endDate.minusWeeks(1);

            BigDecimal totalIncome = transactionRepository.sumAmountByTransactionType(
                    user.getId(), "INCOME", startDate, endDate);
            totalIncome = totalIncome == null ? BigDecimal.ZERO : totalIncome;

            List<String> expenseTypes = java.util.Arrays.asList("EXPENSE", "TRANSFER");
            BigDecimal totalExpense = transactionRepository.sumAmountByTransactionTypes(
                    user.getId(), expenseTypes, startDate, endDate);
            totalExpense = totalExpense == null ? BigDecimal.ZERO : totalExpense;

            Long transactionsWeek = transactionRepository.countNonDeletedTransactionsByPeriod(
                    user.getId(), weekStart, endDate);
            transactionsWeek = transactionsWeek == null ? 0L : transactionsWeek;

            Long transactionsMonth = transactionRepository.countNonDeletedTransactionsByPeriod(
                    user.getId(), monthStart, endDate);
            transactionsMonth = transactionsMonth == null ? 0L : transactionsMonth;

            Long transactionsQuarter = transactionRepository.countNonDeletedTransactionsByPeriod(
                    user.getId(), quarterStart, endDate);
            transactionsQuarter = transactionsQuarter == null ? 0L : transactionsQuarter;

            Long transactionsYear = transactionRepository.countNonDeletedTransactionsByPeriod(
                    user.getId(), yearStart, endDate);
            transactionsYear = transactionsYear == null ? 0L : transactionsYear;

            Sheet summarySheet = workbook.createSheet("Сводная информация");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle negativeStyle = workbook.createCellStyle();
            Font redFont = workbook.createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            negativeStyle.setFont(redFont);
            negativeStyle.setBorderBottom(BorderStyle.THIN);
            negativeStyle.setBorderTop(BorderStyle.THIN);
            negativeStyle.setBorderLeft(BorderStyle.THIN);
            negativeStyle.setBorderRight(BorderStyle.THIN);

            Row titleRow = summarySheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Сводный финансовый отчет");
            titleCell.setCellStyle(titleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

            Row subTitleRow = summarySheet.createRow(1);
            Cell subTitleCell = subTitleRow.createCell(0);
            subTitleCell.setCellValue("Период: " +
                    startDate.format(DATE_FORMATTER) + " - " +
                    endDate.format(DATE_FORMATTER));
            summarySheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));

            Row financeHeaderRow = summarySheet.createRow(3);
            Cell financeHeaderCell = financeHeaderRow.createCell(0);
            financeHeaderCell.setCellValue("ФИНАНСОВЫЙ ИТОГ");
            financeHeaderCell.setCellStyle(headerStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 1));

            Row incomeRow = summarySheet.createRow(4);
            incomeRow.createCell(0).setCellValue("Всего поступлений:");
            Cell incomeCell = incomeRow.createCell(1);
            incomeCell.setCellValue(totalIncome.doubleValue());

            Row expenseRow = summarySheet.createRow(5);
            expenseRow.createCell(0).setCellValue("Всего расходов:");
            Cell expenseCell = expenseRow.createCell(1);
            expenseCell.setCellValue(totalExpense.doubleValue());
            expenseCell.setCellStyle(negativeStyle);

            Row balanceRow = summarySheet.createRow(6);
            balanceRow.createCell(0).setCellValue("Баланс:");
            Cell balanceCell = balanceRow.createCell(1);
            BigDecimal balance = totalIncome.subtract(totalExpense);
            balanceCell.setCellValue(balance.doubleValue());
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balanceCell.setCellStyle(negativeStyle);
            }

            Row transactionHeaderRow = summarySheet.createRow(8);
            Cell transactionHeaderCell = transactionHeaderRow.createCell(0);
            transactionHeaderCell.setCellValue("КОЛИЧЕСТВО ТРАНЗАКЦИЙ ПО ПЕРИОДАМ");
            transactionHeaderCell.setCellStyle(headerStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(8, 8, 0, 1));

            Row weekRow = summarySheet.createRow(9);
            weekRow.createCell(0).setCellValue("За неделю:");
            weekRow.createCell(1).setCellValue(transactionsWeek);

            Row monthRow = summarySheet.createRow(10);
            monthRow.createCell(0).setCellValue("За месяц:");
            monthRow.createCell(1).setCellValue(transactionsMonth);

            Row quarterRow = summarySheet.createRow(11);
            quarterRow.createCell(0).setCellValue("За квартал:");
            quarterRow.createCell(1).setCellValue(transactionsQuarter);

            Row yearRow = summarySheet.createRow(12);
            yearRow.createCell(0).setCellValue("За год:");
            yearRow.createCell(1).setCellValue(transactionsYear);

            Row statusHeaderRow = summarySheet.createRow(14);
            Cell statusHeaderCell = statusHeaderRow.createCell(0);
            statusHeaderCell.setCellValue("СТАТИСТИКА ПО СТАТУСАМ ТРАНЗАКЦИЙ");
            statusHeaderCell.setCellStyle(headerStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(14, 14, 0, 1));

            List<Object[]> statusCounts = transactionRepository.countTransactionsByStatus(user.getId());
            int rowIdx = 15;

            if (statusCounts == null || statusCounts.isEmpty()) {
                Row noDataRow = summarySheet.createRow(rowIdx);
                Cell noDataCell = noDataRow.createCell(0);
                noDataCell.setCellValue("Нет данных");
                summarySheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 1));
            } else {
                for (Object[] result : statusCounts) {
                    Row statusRow = summarySheet.createRow(rowIdx++);
                    statusRow.createCell(0).setCellValue(result[0] != null ? (String) result[0] : "Неизвестный статус");
                    statusRow.createCell(1).setCellValue(result[1] != null ? ((Long) result[1]).intValue() : 0);
                }
            }

            rowIdx += 2;
            Row bankHeaderRow = summarySheet.createRow(rowIdx++);
            Cell bankHeaderCell = bankHeaderRow.createCell(0);
            bankHeaderCell.setCellValue("СТАТИСТИКА ПО БАНКАМ");
            bankHeaderCell.setCellStyle(headerStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 1));

            Row senderHeaderRow = summarySheet.createRow(rowIdx++);
            Cell senderHeaderCell = senderHeaderRow.createCell(0);
            senderHeaderCell.setCellValue("Банки отправители");
            senderHeaderCell.setCellStyle(headerStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 1));

            List<Object[]> senderBankStats = transactionRepository.countTransactionsBySenderBank(user.getId());
            if (senderBankStats == null || senderBankStats.isEmpty()) {
                Row noDataRow = summarySheet.createRow(rowIdx++);
                noDataRow.createCell(0).setCellValue("Нет данных");
                summarySheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 1));
            } else {
                for (Object[] result : senderBankStats) {
                    Row bankRow = summarySheet.createRow(rowIdx++);
                    bankRow.createCell(0).setCellValue(result[0] != null ? (String) result[0] : "Неизвестный банк");
                    bankRow.createCell(0).setCellValue(result[0] != null ? (String) result[0] : "Неизвестный банк");
                    bankRow.createCell(1).setCellValue(result[1] != null ? ((Long) result[1]).intValue() : 0);
                }
            }

            rowIdx += 1;
            Row recipientHeaderRow = summarySheet.createRow(rowIdx++);
            Cell recipientHeaderCell = recipientHeaderRow.createCell(0);
            recipientHeaderCell.setCellValue("Банки получатели");
            recipientHeaderCell.setCellStyle(headerStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 1));

            List<Object[]> recipientBankStats = transactionRepository.countTransactionsByRecipientBank(user.getId());
            if (recipientBankStats == null || recipientBankStats.isEmpty()) {
                Row noDataRow = summarySheet.createRow(rowIdx++);
                noDataRow.createCell(0).setCellValue("Нет данных");
                summarySheet.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 1));
            } else {
                for (Object[] result : recipientBankStats) {
                    Row bankRow = summarySheet.createRow(rowIdx++);
                    bankRow.createCell(0).setCellValue(result[0] != null ? (String) result[0] : "Неизвестный банк");
                    bankRow.createCell(1).setCellValue(result[1] != null ? ((Long) result[1]).intValue() : 0);
                }
            }

            Sheet chartSheet = workbook.createSheet("Диаграммы");

            Row chartTitleRow = chartSheet.createRow(0);
            Cell chartTitleCell = chartTitleRow.createCell(0);
            chartTitleCell.setCellValue("Финансовая статистика");
            chartTitleCell.setCellStyle(titleStyle);
            chartSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            String[] incomeExpenseLabels = {"Доходы", "Расходы"};
            Double[] incomeExpenseValues = {totalIncome.doubleValue(), totalExpense.doubleValue()};

            Row incomeExpenseHeaderRow = chartSheet.createRow(2);
            incomeExpenseHeaderRow.createCell(0).setCellValue("Категория");
            incomeExpenseHeaderRow.createCell(1).setCellValue("Сумма");

            for (int i = 0; i < incomeExpenseLabels.length; i++) {
                Row dataRow = chartSheet.createRow(3 + i);
                dataRow.createCell(0).setCellValue(incomeExpenseLabels[i]);
                dataRow.createCell(1).setCellValue(incomeExpenseValues[i]);
            }

            try {
                XSSFDrawing drawing = ((XSSFSheet)chartSheet).createDrawingPatriarch();
                XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 2, 10, 12);

                XSSFChart chart = drawing.createChart(anchor);
                chart.setTitleText("Доходы и расходы");
                chart.setTitleOverlay(false);

                XDDFChartLegend legend = chart.getOrAddLegend();
                legend.setPosition(LegendPosition.BOTTOM);

                XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
                XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
                valueAxis.setCrosses(AxisCrosses.AUTO_ZERO);
                valueAxis.setCrossBetween(AxisCrossBetween.BETWEEN);

                XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                        (XSSFSheet)chartSheet, new CellRangeAddress(3, 4, 0, 0));

                XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                        (XSSFSheet)chartSheet, new CellRangeAddress(3, 4, 1, 1));

                XDDFChartData data = chart.createData(ChartTypes.BAR, categoryAxis, valueAxis);
                data.setVaryColors(true);
                XDDFChartData.Series series = data.addSeries(categories, values);
                series.setTitle("Суммы", null);

                chart.plot(data);
            } catch (Exception e) {
                logger.error("Ошибка при создании диаграммы доходов/расходов: " + e.getMessage(), e);
                Row errorRow = chartSheet.createRow(10);
                errorRow.createCell(0).setCellValue("Ошибка при создании диаграммы: " + e.getMessage());
            }

            String[] periodLabels = {"Неделя", "Месяц", "Квартал", "Год"};
            Long[] periodValues = {transactionsWeek, transactionsMonth, transactionsQuarter, transactionsYear};

            Row periodHeaderRow = chartSheet.createRow(15);
            periodHeaderRow.createCell(0).setCellValue("Период");
            periodHeaderRow.createCell(1).setCellValue("Количество");

            for (int i = 0; i < periodLabels.length; i++) {
                Row dataRow = chartSheet.createRow(16 + i);
                dataRow.createCell(0).setCellValue(periodLabels[i]);
                dataRow.createCell(1).setCellValue(periodValues[i]);
            }

            try {
                XSSFDrawing drawing = ((XSSFSheet)chartSheet).createDrawingPatriarch();
                XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 15, 10, 25);

                XSSFChart chart = drawing.createChart(anchor);
                chart.setTitleText("Транзакции по периодам");
                chart.setTitleOverlay(false);

                XDDFChartLegend legend = chart.getOrAddLegend();
                legend.setPosition(LegendPosition.BOTTOM);

                XDDFCategoryAxis categoryAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
                XDDFValueAxis valueAxis = chart.createValueAxis(AxisPosition.LEFT);
                valueAxis.setCrosses(AxisCrosses.AUTO_ZERO);
                valueAxis.setCrossBetween(AxisCrossBetween.BETWEEN);

                XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                        (XSSFSheet)chartSheet, new CellRangeAddress(16, 19, 0, 0));

                XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(
                        (XSSFSheet)chartSheet, new CellRangeAddress(16, 19, 1, 1));

                XDDFChartData data = chart.createData(ChartTypes.LINE, categoryAxis, valueAxis);
                data.setVaryColors(false);
                XDDFChartData.Series series = data.addSeries(categories, values);
                series.setTitle("Количество", null);

                chart.plot(data);
            } catch (Exception e) {
                logger.error("Ошибка при создании диаграммы транзакций по периодам: " + e.getMessage(), e);
                Row errorRow = chartSheet.createRow(25);
                errorRow.createCell(0).setCellValue("Ошибка при создании диаграммы: " + e.getMessage());
            }

            summarySheet.setColumnWidth(0, 256 * 30);
            summarySheet.setColumnWidth(1, 256 * 15);
            chartSheet.setColumnWidth(0, 256 * 15);
            chartSheet.setColumnWidth(1, 256 * 15);

            workbook.write(out);
            logger.info("Сводный Excel отчет успешно сформирован для пользователя '{}'", user.getUsername());
            return out.toByteArray();
        }
    }
}
