package com.hubilon.modules.report.application.service;

import com.hubilon.modules.report.application.dto.ReportExportQuery;
import com.hubilon.modules.report.domain.model.CommitInfo;
import com.hubilon.modules.report.domain.model.FileChange;
import com.hubilon.modules.report.domain.model.Report;
import com.hubilon.modules.report.domain.port.in.ReportExportUseCase;
import com.hubilon.modules.report.domain.port.out.ReportQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportService implements ReportExportUseCase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReportQueryPort reportQueryPort;

    @Transactional(readOnly = true)
    @Override
    public byte[] exportToExcel(ReportExportQuery query) {
        List<Report> reports;
        if (query.projectIds() != null && !query.projectIds().isEmpty()) {
            reports = reportQueryPort.findByProjectIdsAndDateRange(
                    query.projectIds(), query.startDate(), query.endDate());
        } else {
            reports = reportQueryPort.findByDateRange(query.startDate(), query.endDate());
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            writeSummarySheet(workbook, reports, query);
            for (Report report : reports) {
                writeProjectSheet(workbook, report);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("엑셀 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void writeSummarySheet(XSSFWorkbook workbook, List<Report> reports, ReportExportQuery query) {
        Sheet sheet = workbook.createSheet("전체 요약");
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowIdx = 0;
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("주간보고 전체 요약");
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        rowIdx++; // 공백 행
        Row periodRow = sheet.createRow(rowIdx++);
        periodRow.createCell(0).setCellValue("기간");
        periodRow.createCell(1).setCellValue(
                query.startDate().format(DATE_FMT) + " ~ " + query.endDate().format(DATE_FMT));

        rowIdx++;
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"프로젝트명", "커밋 수", "요약"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (Report report : reports) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(report.getProjectName());
            row.createCell(1).setCellValue(report.getCommits() == null ? 0 : report.getCommits().size());
            row.createCell(2).setCellValue(report.getEffectiveSummary() != null ? report.getEffectiveSummary() : "");
        }

        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 3000);
        sheet.setColumnWidth(2, 20000);
    }

    private void writeProjectSheet(XSSFWorkbook workbook, Report report) {
        String sheetName = sanitizeSheetName(report.getProjectName());
        Sheet sheet = workbook.createSheet(sheetName);
        CellStyle headerStyle = createHeaderStyle(workbook);

        int rowIdx = 0;
        Row titleRow = sheet.createRow(rowIdx++);
        titleRow.createCell(0).setCellValue(report.getProjectName() + " — 커밋 목록");

        rowIdx++;
        Row headerRow = sheet.createRow(rowIdx++);
        String[] commitHeaders = {"SHA", "작성자", "날짜", "메시지"};
        for (int i = 0; i < commitHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(commitHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        List<CommitInfo> commits = report.getCommits() != null ? report.getCommits() : Collections.emptyList();
        for (CommitInfo commit : commits) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(commit.getSha() != null ? commit.getSha().substring(0, Math.min(8, commit.getSha().length())) : "");
            row.createCell(1).setCellValue(commit.getAuthorName() != null ? commit.getAuthorName() : "");
            row.createCell(2).setCellValue(commit.getCommittedAt() != null ? commit.getCommittedAt().format(DATETIME_FMT) : "");
            row.createCell(3).setCellValue(commit.getMessage() != null ? commit.getMessage() : "");
        }

        rowIdx++;
        Row fileHeaderRow = sheet.createRow(rowIdx++);
        fileHeaderRow.createCell(0).setCellValue("변경 파일 목록");

        Row fileColHeaderRow = sheet.createRow(rowIdx++);
        String[] fileHeaders = {"파일 경로", "상태", "추가 라인", "삭제 라인"};
        for (int i = 0; i < fileHeaders.length; i++) {
            Cell cell = fileColHeaderRow.createCell(i);
            cell.setCellValue(fileHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        for (CommitInfo commit : commits) {
            if (commit.getFileChanges() == null) continue;
            for (FileChange fc : commit.getFileChanges()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(fc.getNewPath() != null ? fc.getNewPath() : fc.getOldPath());
                row.createCell(1).setCellValue(resolveFileStatus(fc));
                row.createCell(2).setCellValue(fc.getAddedLines());
                row.createCell(3).setCellValue(fc.getRemovedLines());
            }
        }

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 3000);
        sheet.setColumnWidth(3, 15000);
    }

    private String resolveFileStatus(FileChange fc) {
        if (fc.isNewFile()) return "추가";
        if (fc.isDeletedFile()) return "삭제";
        if (fc.isRenamedFile()) return "이름변경";
        return "수정";
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private String sanitizeSheetName(String name) {
        if (name == null) return "sheet";
        String sanitized = name.replaceAll("[\\\\/:*?\\[\\]]", "_");
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }
}
