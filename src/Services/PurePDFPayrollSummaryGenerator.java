package Services;

import Services.ReportService.PayrollSummaryEntry;
import Services.ReportService.MonthlyPayrollSummaryReport;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.Color;
import java.io.File;
import java.util.List;
import java.io.IOException;

/**
 * Pure Java PDF Payroll Summary Generator with left-aligned title and wider columns
 * Creates professional payroll summary reports matching MotorPH format
 * Uses iText directly for PDF generation with landscape orientation
 */
public class PurePDFPayrollSummaryGenerator {
    
    // MotorPH Brand Colors
    private static final Color MOTORPH_BLUE = new Color(31, 56, 100);
    private static final Color HEADER_DARK_BLUE = new Color(52, 73, 94);
    private static final Color WHITE = Color.WHITE;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy");
    
    /**
     * Generates a professional payroll summary PDF using pure Java
     * @param summaryData Payroll summary data from ReportService
     * @param period Pay period (e.g., "2024-06")
     * @param department Department filter (e.g., "All" or specific department)
     * @param generatedBy Who generated the report
     * @param outputPath Output file path
     * @return true if successful, false if failed
     */
    public static boolean generatePayrollSummaryPDF(MonthlyPayrollSummaryReport summaryData, 
            String period, String department, String generatedBy, String outputPath) {
        
        // Validate input parameters
        if (summaryData == null) {
            System.err.println("Error generating Payroll Summary PDF: summaryData cannot be null");
            return false;
        }
        
        if (outputPath == null || outputPath.trim().isEmpty()) {
            System.err.println("Error generating Payroll Summary PDF: outputPath cannot be null or empty");
            return false;
        }

        // Validate and prepare output path
        File outputFile = new File(outputPath);
        
        // Check if the path is valid and writable
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            try {
                if (!parentDir.mkdirs()) {
                    System.err.println("Error creating directory path: " + parentDir.getAbsolutePath());
                    return false;
                }
            } catch (SecurityException e) {
                System.err.println("Security exception when trying to create directories: " + e.getMessage());
                return false;
            }
        }
        
        // Additional validation: check if parent directory is writable
        if (parentDir != null && !parentDir.canWrite()) {
            System.err.println("Error: Parent directory is not writable: " + parentDir.getAbsolutePath());
            return false;
        }
        
        // Check for invalid paths (like absolute paths on non-existent drives)
        try {
            // Test if we can create the file by attempting to open it
            FileOutputStream testStream = new FileOutputStream(outputFile);
            testStream.close();
            // If we get here, the path is valid, but we need to delete the empty file
            if (outputFile.exists() && outputFile.length() == 0) {
                outputFile.delete();
            }
        } catch (IOException | SecurityException e) {
            System.err.println("Error: Cannot write to specified path: " + outputPath + " - " + e.getMessage());
            return false;
        }

        // Use landscape orientation for wide tables
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 50);
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            
            // Add page events for headers and footers
            HeaderFooterPageEvent pageEvent = new HeaderFooterPageEvent(period, department, generatedBy);
            writer.setPageEvent(pageEvent);
            
            document.open();
            
            // Add company header with left-aligned title
            addCompanyHeaderWithLeftAlignedTitle(document, period, department);
            
            // Add main payroll table with wider columns
            addPayrollTableWithWiderColumns(document, summaryData.getPayrollEntries());
            
            // Add totals row
            addTotalsRow(document, summaryData.getTotals());
            
            document.close();
            System.out.println("Payroll Summary PDF generated successfully with left-aligned title: " + outputPath);
            return true;
            
        } catch (Exception e) {
            System.err.println("Error generating Payroll Summary PDF: " + e.getMessage());
            // Clean up any partially created file
            if (outputFile.exists()) {
                try {
                    outputFile.delete();
                } catch (Exception deleteEx) {
                    System.err.println("Could not delete partially created file: " + deleteEx.getMessage());
                }
            }
            return false;
        }
    }
    
    /**
     * Generate unique filename with version number if file already exists
     * @param basePath The original file path
     * @return A unique file path, or null if basePath is null or empty
     */
    public static String generateUniqueFilePath(String basePath) {
        // Handle null or empty input
        if (basePath == null || basePath.trim().isEmpty()) {
            return null;
        }
        
        File file = new File(basePath);
        if (!file.exists()) {
            return basePath;
        }
        
        String directory = file.getParent();
        String filename = file.getName();
        
        String name;
        String extension;
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            name = filename.substring(0, lastDotIndex);
            extension = filename.substring(lastDotIndex);
        } else {
            name = filename;
            extension = "";
        }
        
        int version = 1;
        String newPath;
        do {
            String versionedName = name + "(" + version + ")" + extension;
            newPath = (directory != null ? directory + File.separator : "") + versionedName;
            version++;
        } while (new File(newPath).exists());
        
        System.out.println("File already exists, using versioned filename: " + newPath);
        return newPath;
    }
    
    /**
     * Adds MotorPH company header with logo and left-aligned title
     */
    private static void addCompanyHeaderWithLeftAlignedTitle(Document document, String period, String department) throws DocumentException {
        try {
            // Main header table with logo, company info, and period info
            PdfPTable headerTable = new PdfPTable(3);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{15, 55, 30});
            headerTable.setSpacingAfter(20f);
            
            // Logo cell (left)
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setFixedHeight(80);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            Image logo = tryLoadLogo();
            if (logo != null) {
                logo.scaleToFit(70, 70);
                logo.setAlignment(Image.ALIGN_CENTER);
                logoCell.addElement(logo);
            } else {
                // Fallback logo text
                Paragraph logoText = new Paragraph("MotorPH", 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, MOTORPH_BLUE));
                logoText.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(logoText);
            }
            headerTable.addCell(logoCell);
            
            // Company info and title (center) - changed to left alignment
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            titleCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            
            Paragraph companyName = new Paragraph("MotorPH", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, MOTORPH_BLUE));
            companyName.setAlignment(Element.ALIGN_LEFT);
            
            Paragraph tagline = new Paragraph("The Filipino's Choice", 
                FontFactory.getFont(FontFactory.HELVETICA, 12, MOTORPH_BLUE));
            tagline.setAlignment(Element.ALIGN_LEFT);
            
            Paragraph reportTitle = new Paragraph("MONTHLY PAYROLL SUMMARY REPORT", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, MOTORPH_BLUE));
            reportTitle.setAlignment(Element.ALIGN_LEFT);
            reportTitle.setSpacingBefore(10f);
            
            titleCell.addElement(companyName);
            titleCell.addElement(tagline);
            titleCell.addElement(reportTitle);
            headerTable.addCell(titleCell);
            
            // Period and department info (right)
            PdfPCell infoCell = new PdfPCell();
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10, MOTORPH_BLUE);
            
            Paragraph periodInfo = new Paragraph("Period: " + period, infoFont);
            periodInfo.setAlignment(Element.ALIGN_RIGHT);
            
            Paragraph deptInfo = new Paragraph("Department: " + department, infoFont);
            deptInfo.setAlignment(Element.ALIGN_RIGHT);
            
            infoCell.addElement(periodInfo);
            infoCell.addElement(deptInfo);
            headerTable.addCell(infoCell);
            
            document.add(headerTable);
            
        } catch (Exception e) {
            System.err.println("Warning: Could not add company header - " + e.getMessage());
            throw new DocumentException(e);
        }
    }
    
    /**
     * Attempts to load MotorPH logo from various locations
     */
    private static Image tryLoadLogo() {
        try {
            return Image.getInstance(PurePDFPayrollSummaryGenerator.class.getResource("/media/OG Logo _ 100X124.png"));
        } catch (Exception e1) {
            try {
                return Image.getInstance("src/media/OG Logo _ 100X124.png");
            } catch (Exception e2) {
                try {
                    return Image.getInstance("media/OG Logo _ 100X124.png");
                } catch (Exception e3) {
                    System.err.println("Could not load MotorPH logo from any location");
                    return null;
                }
            }
        }
    }
    
    /**
     * Adds the main payroll summary table with wider columns to prevent number wrapping
     */
    private static void addPayrollTableWithWiderColumns(Document document, List<PayrollSummaryEntry> records) throws DocumentException {
        // Wider column widths - significantly increased to prevent number wrapping
        float[] columnWidths = {
            8f,   // Emp ID - increased from 6f
            18f,  // Employee Name - increased from 15f
            22f,  // Position - increased from 20f
            15f,  // Department - increased from 12f
            15f,  // Base Salary - increased from 10f
            12f,  // Leaves - increased from 8f
            12f,  // Overtime - increased from 8f
            15f,  // Gross Income - increased from 12f
            14f,  // Total Benefits - increased from 10f
            12f,  // SSS - increased from 8f
            12f,  // PhilHealth - increased from 8f
            12f,  // Pag-Ibig - increased from 8f
            14f,  // Withholding Tax - increased from 8f
            16f   // Net Pay - increased from 12f
        };
        
        PdfPTable table = new PdfPTable(columnWidths.length);
        table.setWidthPercentage(100);
        table.setWidths(columnWidths);
        table.setSpacingAfter(10f);
        
        // Add header row with professional styling
        String[] headers = {
            "Emp ID", "Employee Name", "Position", "Department", 
            "Base Salary", "Leaves", "Overtime", "Gross Income",
            "Total Benefits", "SSS", "PhilHealth", "Pag-Ibig", "With. Tax", "Net Pay"
        };
        
        for (String header : headers) {
            addStyledHeaderCell(table, header);
        }
        
        // Add data rows with alternating colors
        boolean alternateRow = false;
        if (records != null) {
            for (PayrollSummaryEntry record : records) {
                Color rowColor = alternateRow ? new Color(248, 248, 248) : WHITE;
                
                addStyledDataCell(table, record.getEmployeeId().toString(), rowColor, Element.ALIGN_CENTER);
                addStyledDataCell(table, record.getEmployeeName(), rowColor, Element.ALIGN_LEFT);
                addStyledDataCell(table, record.getPosition(), rowColor, Element.ALIGN_LEFT);
                addStyledDataCell(table, record.getDepartment(), rowColor, Element.ALIGN_LEFT);
                
                // Enhanced currency formatting - using proper currency symbols and better formatting
                addStyledCurrencyCell(table, record.getBaseSalary(), rowColor);
                addStyledCurrencyCell(table, record.getLeaves(), rowColor);
                addStyledCurrencyCell(table, record.getOvertime(), rowColor);
                addStyledCurrencyCell(table, record.getGrossIncome(), rowColor);
                addStyledCurrencyCell(table, record.getTotalBenefits(), rowColor);
                addStyledCurrencyCell(table, record.getSssContribution(), rowColor);
                addStyledCurrencyCell(table, record.getPhilhealthContribution(), rowColor);
                addStyledCurrencyCell(table, record.getPagibigContribution(), rowColor);
                addStyledCurrencyCell(table, record.getWithholdingTax(), rowColor);
                addStyledCurrencyCell(table, record.getNetPay(), rowColor);
                
                alternateRow = !alternateRow;
            }
        }
        
        document.add(table);
    }
    
    /**
     * Adds totals row at the bottom with wider columns
     */
    private static void addTotalsRow(Document document, Services.ReportService.PayrollTotals totals) throws DocumentException {
        if (totals == null) return;
        
        // Create totals table with wider column proportions
        PdfPTable totalsTable = new PdfPTable(14);
        totalsTable.setWidthPercentage(100);
        // Using the same wider column proportions as the main table
        totalsTable.setWidths(new float[]{8f, 18f, 22f, 15f, 15f, 12f, 12f, 15f, 14f, 12f, 12f, 12f, 14f, 16f});
        totalsTable.setSpacingBefore(5f);
        
        // Add "TOTAL" label and values
        addTotalCell(totalsTable, "TOTAL", 4); // Span first 4 columns
        addTotalCurrencyCell(totalsTable, totals.getTotalBaseSalary());
        addTotalCurrencyCell(totalsTable, totals.getTotalLeaves());
        addTotalCurrencyCell(totalsTable, totals.getTotalOvertime());
        addTotalCurrencyCell(totalsTable, totals.getTotalGrossIncome());
        addTotalCurrencyCell(totalsTable, totals.getTotalBenefits());
        addTotalCell(totalsTable, "", 1); // SSS
        addTotalCell(totalsTable, "", 1); // PhilHealth
        addTotalCell(totalsTable, "", 1); // Pag-Ibig
        addTotalCell(totalsTable, "", 1); // Tax
        addTotalCurrencyCell(totalsTable, totals.getTotalNetPay());
        
        document.add(totalsTable);
    }
    
    // HELPER METHODS FOR STYLING
    
    private static void addStyledHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(HEADER_DARK_BLUE);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(1f);
        cell.setBorderColor(Color.WHITE);
        
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Paragraph para = new Paragraph(text, font);
        para.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(para);
        
        table.addCell(cell);
    }
    
    private static void addStyledDataCell(PdfPTable table, String text, Color backgroundColor, int alignment) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(backgroundColor);
        cell.setPadding(6); // Slightly more padding for better readability
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(Color.LIGHT_GRAY);
        
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, MOTORPH_BLUE);
        Paragraph para = new Paragraph(text != null ? text : "", font);
        cell.addElement(para);
        
        table.addCell(cell);
    }
    
    private static void addStyledCurrencyCell(PdfPTable table, BigDecimal amount, Color backgroundColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(backgroundColor);
        cell.setPadding(6); // Slightly more padding
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(Color.LIGHT_GRAY);
        
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, MOTORPH_BLUE);
        // Enhanced currency formatting - ensure no wrapping with proper spacing
        String formattedAmount = formatCurrencyForPDF(amount);
        Paragraph para = new Paragraph(formattedAmount, font);
        cell.addElement(para);
        
        table.addCell(cell);
    }
    
    private static void addTotalCell(PdfPTable table, String text, int colspan) {
        PdfPCell cell = new PdfPCell();
        cell.setColspan(colspan);
        cell.setBackgroundColor(HEADER_DARK_BLUE);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(1f);
        cell.setBorderColor(Color.WHITE);
        
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Paragraph para = new Paragraph(text, font);
        cell.addElement(para);
        
        table.addCell(cell);
    }
    
    private static void addTotalCurrencyCell(PdfPTable table, BigDecimal amount) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(HEADER_DARK_BLUE);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(1f);
        cell.setBorderColor(Color.WHITE);
        
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Paragraph para = new Paragraph(formatCurrencyForPDF(amount), font);
        cell.addElement(para);
        
        table.addCell(cell);
    }
    
    /**
     * Enhanced currency formatting for PDF - prevents wrapping and ensures proper display
     */
    private static String formatCurrencyForPDF(BigDecimal amount) {
        if (amount == null) return "₱0.00";
        
        // Format with Philippine peso symbol and proper spacing to prevent wrapping
        return String.format("₱%,.2f", amount);
    }
    
    private static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
    
    /**
     * Page Event Handler for Headers and Footers
     */
    static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private String period;
        private String department;
        private String generatedBy;
        
        public HeaderFooterPageEvent(String period, String department, String generatedBy) {
            this.period = period;
            this.department = department;
            this.generatedBy = generatedBy;
        }
        
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                // Add dynamic page number in top-right corner
                PdfContentByte cb = writer.getDirectContent();
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                
                // Position for page number (top right)
                float x = document.right() - 50;
                float y = document.top() + 10;
                
                // Add page number
                String pageText = "Page " + writer.getPageNumber();
                cb.beginText();
                cb.setFontAndSize(bf, 10);
                cb.setColorFill(MOTORPH_BLUE);
                cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, pageText, x, y, 0);
                cb.endText();
                
                // Add footer with fixed width
                PdfPTable footerTable = new PdfPTable(2);
                footerTable.setTotalWidth(document.right() - document.left());
                footerTable.setWidths(new float[]{70, 30});
                footerTable.setLockedWidth(true);
                
                // Left side - Generated by info
                PdfPCell leftCell = new PdfPCell();
                leftCell.setBorder(Rectangle.NO_BORDER);
                leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                
                Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, MOTORPH_BLUE);
                Paragraph generatedInfo = new Paragraph(
                    "Generated by: " + generatedBy + " on " + DATE_FORMAT.format(new Date()), 
                    footerFont
                );
                leftCell.addElement(generatedInfo);
                
                Paragraph disclaimer = new Paragraph(
                    "THIS IS A SYSTEM GENERATED REPORT AND DOES NOT REQUIRE A SIGNATURE", 
                    FontFactory.getFont(FontFactory.HELVETICA, 7, MOTORPH_BLUE)
                );
                leftCell.addElement(disclaimer);
                
                footerTable.addCell(leftCell);
                
                // Right side - MotorPH branding
                PdfPCell rightCell = new PdfPCell();
                rightCell.setBorder(Rectangle.NO_BORDER);
                rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                
                Paragraph brandInfo = new Paragraph("MotorPH - The Filipino's Choice", 
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MOTORPH_BLUE));
                brandInfo.setAlignment(Element.ALIGN_RIGHT);
                rightCell.addElement(brandInfo);
                
                Paragraph reportType = new Paragraph("MONTHLY PAYROLL SUMMARY REPORT", 
                    FontFactory.getFont(FontFactory.HELVETICA, 7, MOTORPH_BLUE));
                reportType.setAlignment(Element.ALIGN_RIGHT);
                rightCell.addElement(reportType);
                
                footerTable.addCell(rightCell);
                
                // Position footer at bottom of page
                footerTable.writeSelectedRows(0, -1, 
                    document.left(), 
                    document.bottom() - 20, 
                    writer.getDirectContent()
                );
                
            } catch (Exception e) {
                System.err.println("Error adding footer: " + e.getMessage());
            }
        }
    }
}