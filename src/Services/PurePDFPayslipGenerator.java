package Services;

import Services.ReportService.PayslipDetails;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.Color;

/**
 * Pure Java PDF Payslip Generator - No JRXML dependencies as it causes errors
 * Uses iText directly for PDF generation
 */
public class PurePDFPayslipGenerator {
    
    private static final Color MOTORPH_BLUE = new Color(31, 56, 100);    // #1F3864
    private static final Color MOTORPH_ORANGE = new Color(237, 125, 49); // #ED7D31
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy");
    
    /**
     * Safely formats any date object to string
     */
    private String formatDate(Object dateObj) {
        if (dateObj == null) return "N/A";
        
        try {
            // Handle java.time.LocalDate
            if (dateObj instanceof java.time.LocalDate) {
                java.time.LocalDate localDate = (java.time.LocalDate) dateObj;
                return localDate.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy"));
            }
            // Handle java.util.Date
            else if (dateObj instanceof java.util.Date) {
                return DATE_FORMAT.format((java.util.Date) dateObj);
            }
            // Handle java.sql.Date
            else if (dateObj instanceof java.sql.Date) {
                return DATE_FORMAT.format((java.sql.Date) dateObj);
            }
            // Handle java.time.LocalDateTime
            else if (dateObj instanceof java.time.LocalDateTime) {
                java.time.LocalDateTime localDateTime = (java.time.LocalDateTime) dateObj;
                return localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy"));
            }
            // Fallback - try toString
            else {
                String dateStr = dateObj.toString();
                // If it looks like a date, parse it
                if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                    return java.time.LocalDate.parse(dateStr.substring(0, 10)).format(
                        java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy"));
                }
                return dateStr;
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not format date object: " + dateObj + " (" + dateObj.getClass() + ")");
            return dateObj.toString();
        }
    }
    
    /**
     * Generates a professional payslip PDF using pure Java
     * @param payslip Payslip data from database
     * @param outputPath Output file path
     * @return true if successful, false if failed
     */
    public boolean generatePayslipPDF(PayslipDetails payslip, String outputPath) {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            document.open();
            
            // Add content to PDF
            addHeader(document, payslip);
            addEmployeeInfo(document, payslip);
            addPayrollDetails(document, payslip);
            addFooter(document);
            
            document.close();
            System.out.println("✅ PDF generated successfully: " + outputPath);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error generating PDF: " + e.getMessage());
            e.printStackTrace();
            if (document.isOpen()) {
                document.close();
            }
            return false;
        }
    }
    
    /**
     * Adds MotorPH header with logo and title
     */
    private void addHeader(Document document, PayslipDetails payslip) throws DocumentException {
        try {
            // Company header
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{30, 70});
            headerTable.setSpacingAfter(20f);
            
            // Logo cell (left side)
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setFixedHeight(80);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            
            // Try to load logo from different possible locations
            Image logo = tryLoadLogo();
            if (logo != null) {
                logo.scaleToFit(80, 80);
                logo.setAlignment(Image.ALIGN_CENTER);
                logoCell.addElement(logo);
            } else {
                // Fallback to text if logo can't be loaded
                Paragraph logoText = new Paragraph("MOTORPH", 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, MOTORPH_BLUE));
                logoText.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(logoText);
            }
            
            headerTable.addCell(logoCell);
            
            // Company info (right side)
            PdfPCell companyCell = new PdfPCell();
            companyCell.setBorder(Rectangle.NO_BORDER);
            companyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            
            Paragraph companyName = new Paragraph("MotorPH", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, MOTORPH_BLUE));
            
            Paragraph tagline = new Paragraph("The Filipino's Choice", 
                FontFactory.getFont(FontFactory.HELVETICA, 12, MOTORPH_BLUE));
            
            Paragraph title = new Paragraph("Employee Payslip", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, MOTORPH_BLUE));
            title.setAlignment(Element.ALIGN_RIGHT);
            
            companyCell.addElement(companyName);
            companyCell.addElement(tagline);
            companyCell.addElement(Chunk.NEWLINE);
            companyCell.addElement(title);
            headerTable.addCell(companyCell);
            
            document.add(headerTable);
            
        } catch (Exception e) {
            System.err.println("Warning: Could not add header - " + e.getMessage());
            throw new DocumentException(e);
        }
    }
    
    /**
     * Attempts to load logo from various possible locations
     */
    private Image tryLoadLogo() {
        try {
            // Try classpath resource first
            return Image.getInstance(getClass().getResource("/media/OG Logo _ 100X124.png"));
        } catch (Exception e1) {
            try {
                // Try file path relative to project
                return Image.getInstance("src/media/OG Logo _ 100X124.png");
            } catch (Exception e2) {
                try {
                    // Try absolute path
                    return Image.getInstance("media/OG Logo _ 100X124.png");
                } catch (Exception e3) {
                    System.err.println("Could not load logo from any location");
                    return null;
                }
            }
        }
    }
    
    /**
     * Adds payslip period information
     */
    private void addPayslipInfo(Document document, PayslipDetails payslip) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{25, 25, 25, 25});
        infoTable.setSpacingAfter(15f);
        
        // Headers with orange background
        addStyledCell(infoTable, "Period Start Date", MOTORPH_ORANGE, Color.WHITE, true);
        addStyledCell(infoTable, "Period End Date", MOTORPH_ORANGE, Color.WHITE, true);
        addStyledCell(infoTable, "Pay Date", MOTORPH_ORANGE, Color.WHITE, true);
        addStyledCell(infoTable, "Payslip No", MOTORPH_ORANGE, Color.WHITE, true);
        
        // Values - using safe date formatting
        addStyledCell(infoTable, formatDate(payslip.getPeriodStartDate()), Color.WHITE, MOTORPH_BLUE, false);
        addStyledCell(infoTable, formatDate(payslip.getPeriodEndDate()), Color.WHITE, MOTORPH_BLUE, false);
        addStyledCell(infoTable, formatDate(payslip.getPayDate()), Color.WHITE, MOTORPH_BLUE, false);
        addStyledCell(infoTable, payslip.getPayslipNo(), Color.WHITE, MOTORPH_BLUE, false);
        
        document.add(infoTable);
    }
    
    /**
     * Adds employee information section
     */
    private void addEmployeeInfo(Document document, PayslipDetails payslip) throws DocumentException {
        addPayslipInfo(document, payslip);
        
        PdfPTable empTable = new PdfPTable(4);
        empTable.setWidthPercentage(100);
        empTable.setWidths(new float[]{15, 35, 15, 35});
        
        // Employee details
        addLabelValueRow(empTable, "Employee ID:", payslip.getEmployeeId().toString(), "TIN:", safeString(payslip.getTin()));
        addLabelValueRow(empTable, "Name:", payslip.getEmployeeName(), "SSS No:", safeString(payslip.getSssNo()));
        addLabelValueRow(empTable, "Title:", payslip.getEmployeePosition(), "PagIbig No:", safeString(payslip.getPagibigNo()));
        addLabelValueRow(empTable, "Department:", payslip.getDepartment(), "Philhealth No:", safeString(payslip.getPhilhealthNo()));
        
        document.add(empTable);
        document.add(new Paragraph(" ")); // spacing
    }
    
    /**
     * Adds payroll details in two-column layout (Earnings and Deductions)
     */
    private void addPayrollDetails(Document document, PayslipDetails payslip) throws DocumentException {
        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{50, 50});
        
        // Left column - Earnings
        PdfPCell earningsCell = new PdfPCell();
        earningsCell.setBorder(Rectangle.BOX);
        earningsCell.setPadding(10);
        
        PdfPTable earningsTable = createEarningsTable(payslip);
        earningsCell.addElement(earningsTable);
        mainTable.addCell(earningsCell);
        
        // Right column - Deductions
        PdfPCell deductionsCell = new PdfPCell();
        deductionsCell.setBorder(Rectangle.BOX);
        deductionsCell.setPadding(10);
        
        PdfPTable deductionsTable = createDeductionsTable(payslip);
        deductionsCell.addElement(deductionsTable);
        mainTable.addCell(deductionsCell);
        
        document.add(mainTable);
        document.add(new Paragraph(" ")); // spacing
        
        // Benefits section
        addBenefitsSection(document, payslip);
        
        // Summary section
        addSummarySection(document, payslip);
    }
    
    /**
     * Creates earnings table
     */
    private PdfPTable createEarningsTable(PayslipDetails payslip) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60, 40});
        
        // Header
        addStyledCell(table, "EARNINGS", MOTORPH_ORANGE, Color.WHITE, true, 2);
        
        // Earnings details
        addAmountRow(table, "Monthly Rate:", payslip.getMonthlyRate());
        addAmountRow(table, "Daily Rate:", payslip.getDailyRate());
        addValueRow(table, "Days Worked:", payslip.getDaysWorked().toString());
        addValueRow(table, "Leaves Taken:", payslip.getLeavesTaken().toString());
        addValueRow(table, "Overtime Hours:", payslip.getOvertimeHours().toString());
        
        // Gross income total
        addStyledCell(table, "GROSS INCOME", MOTORPH_BLUE, Color.WHITE, true);
        addStyledCell(table, formatCurrency(payslip.getGrossIncome()), MOTORPH_BLUE, Color.WHITE, true);
        
        return table;
    }
    
    /**
     * Creates deductions table
     */
    private PdfPTable createDeductionsTable(PayslipDetails payslip) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{60, 40});
        
        // Header
        addStyledCell(table, "DEDUCTIONS", MOTORPH_ORANGE, Color.WHITE, true, 2);
        
        // Deductions details
        addAmountRow(table, "Social Security System:", payslip.getSocialSecuritySystem());
        addAmountRow(table, "Philhealth:", payslip.getPhilhealth());
        addAmountRow(table, "PagIbig:", payslip.getPagIbig());
        addAmountRow(table, "Withholding Tax:", payslip.getWithholdingTax());
        
        // Add empty rows to match earnings table height
        addValueRow(table, "", "");
        addValueRow(table, "", "");
        addValueRow(table, "", "");

        // Total deductions
        addStyledCell(table, "TOTAL DEDUCTIONS", MOTORPH_BLUE, Color.WHITE, true);
        addStyledCell(table, formatCurrency(payslip.getTotalDeductions()), MOTORPH_BLUE, Color.WHITE, true);
        
        return table;
    }
    
    /**
     * Adds benefits section
     */
    private void addBenefitsSection(Document document, PayslipDetails payslip) throws DocumentException {
        PdfPTable benefitsTable = new PdfPTable(2);
        benefitsTable.setWidthPercentage(100);
        benefitsTable.setWidths(new float[]{70, 30});
        
        // Header
        addStyledCell(benefitsTable, "BENEFITS", MOTORPH_ORANGE, Color.WHITE, true, 2);
        
        // Benefits details
        addAmountRow(benefitsTable, "Rice Subsidy:", payslip.getRiceSubsidy());
        addAmountRow(benefitsTable, "Phone Allowance:", payslip.getPhoneAllowance());
        addAmountRow(benefitsTable, "Clothing Allowance:", payslip.getClothingAllowance());
        
        // Total benefits
        addStyledCell(benefitsTable, "TOTAL BENEFITS", MOTORPH_BLUE, Color.WHITE, true);
        addStyledCell(benefitsTable, formatCurrency(payslip.getTotalBenefits()), MOTORPH_BLUE, Color.WHITE, true);
        
        document.add(benefitsTable);
        document.add(new Paragraph(" ")); // spacing
    }
    
    /**
     * Adds final summary with net pay
     */
    private void addSummarySection(Document document, PayslipDetails payslip) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(1);
        summaryTable.setWidthPercentage(100);
        
        // Net pay - large and prominent
        PdfPCell netPayCell = new PdfPCell();
        netPayCell.setBackgroundColor(MOTORPH_BLUE);
        netPayCell.setPadding(15);
        netPayCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        
        Font netPayFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
        Paragraph netPayPara = new Paragraph("NET PAY: " + formatCurrency(payslip.getNetPay()), netPayFont);
        netPayPara.setAlignment(Element.ALIGN_CENTER);
        netPayCell.addElement(netPayPara);
        
        summaryTable.addCell(netPayCell);
        document.add(summaryTable);
    }
    
    /**
     * Adds footer
     */
    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" ")); // spacing
        
        Paragraph footer = new Paragraph(
            "THIS IS A SYSTEM GENERATED PAYSLIP AND DOES NOT REQUIRE A SIGNATURE.",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MOTORPH_BLUE)
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }
    
    // HELPER METHODS
    
    private void addStyledCell(PdfPTable table, String text, Color bgColor, Color textColor, boolean bold) {
        addStyledCell(table, text, bgColor, textColor, bold, 1);
    }
    
    private void addStyledCell(PdfPTable table, String text, Color bgColor, Color textColor, boolean bold, int colspan) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Font font = bold ? 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, textColor) :
            FontFactory.getFont(FontFactory.HELVETICA, 10, textColor);
            
        Paragraph para = new Paragraph(text, font);
        para.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(para);
        
        table.addCell(cell);
    }
    
    private void addLabelValueRow(PdfPTable table, String label1, String value1, String label2, String value2) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MOTORPH_BLUE);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MOTORPH_BLUE);
        
        table.addCell(new Phrase(label1, labelFont));
        table.addCell(new Phrase(value1, valueFont));
        table.addCell(new Phrase(label2, labelFont));
        table.addCell(new Phrase(value2, valueFont));
    }
    
    private void addAmountRow(PdfPTable table, String label, BigDecimal amount) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MOTORPH_BLUE);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MOTORPH_BLUE);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(formatCurrency(amount), valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }
    
    private void addValueRow(PdfPTable table, String label, String value) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MOTORPH_BLUE);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MOTORPH_BLUE);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "PHP 0.00";
        return String.format("PHP %,.2f", amount);
    }
    
    private String safeString(String str) {
        return str != null ? str : "N/A";
    }
    
    /**
     * Factory method for easy integration
     */
    public static boolean generatePayslip(PayslipDetails payslip, String outputPath) {
        PurePDFPayslipGenerator generator = new PurePDFPayslipGenerator();
        return generator.generatePayslipPDF(payslip, outputPath);
    }
}