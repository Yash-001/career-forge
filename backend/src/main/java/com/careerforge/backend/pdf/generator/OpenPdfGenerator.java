package com.careerforge.backend.pdf.generator;

import com.careerforge.backend.pdf.dto.ResumeVersionData;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * ATS-friendly single-column PDF template.
 *
 * Section order: Name → Contact → Summary → Experience → Education → Skills
 *
 * ATS constraints honoured:
 * - Single column, no tables for layout, no graphics, no sidebars
 * - Standard Helvetica font (machine-readable, universally supported)
 * - Clear uppercase section headings with a thin rule
 * - SpacingBefore/SpacingAfter on every paragraph — no bare Chunk.NEWLINE gaps
 * - Empty sections produce no heading
 * - Long descriptions wrap naturally via iText paragraph flow
 */
@Component
public class OpenPdfGenerator implements PdfGenerator {

    private static final float MARGIN_H = 54f;  // 0.75 in
    private static final float MARGIN_V = 47f;  // 0.65 in
    private static final float LEADING  = 14f;

    private static final Color GRAY_DARK = new Color(80, 80, 80);
    private static final Color GRAY_RULE = new Color(160, 160, 160);

    private static final Font FONT_NAME    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    20, Color.BLACK);
    private static final Font FONT_CONTACT = FontFactory.getFont(FontFactory.HELVETICA,          9, GRAY_DARK);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    10, Color.BLACK);
    private static final Font FONT_BODY    = FontFactory.getFont(FontFactory.HELVETICA,         10, Color.BLACK);
    private static final Font FONT_BOLD    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    10, Color.BLACK);
    private static final Font FONT_ITALIC  = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,  9, GRAY_DARK);

    @Override
    public byte[] generate(ResumeVersionData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER, MARGIN_H, MARGIN_H, MARGIN_V, MARGIN_V);
            PdfWriter.getInstance(doc, out);
            doc.open();

            renderHeader(doc, data);
            renderSummary(doc, data.professionalSummary());
            renderExperiences(doc, data.experiences());
            renderEducations(doc, data.educations());
            renderSkills(doc, data.skills());

            // OpenPDF requires at least one element; add an invisible placeholder
            // when the resume has no renderable content (e.g. all-null test data).
            doc.add(new Paragraph(" "));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private void renderHeader(Document doc, ResumeVersionData data) throws DocumentException {
        if (hasText(data.fullName())) {
            Paragraph p = new Paragraph(data.fullName(), FONT_NAME);
            p.setSpacingAfter(3f);
            doc.add(p);
        }

        String contact = buildContactLine(data);
        if (!contact.isEmpty()) {
            Paragraph p = new Paragraph(contact, FONT_CONTACT);
            p.setSpacingAfter(8f);
            doc.add(p);
        }
    }

    private String buildContactLine(ResumeVersionData data) {
        StringBuilder sb = new StringBuilder();
        appendContact(sb, data.email());
        appendContact(sb, data.phone());
        appendContact(sb, data.location());
        appendContact(sb, data.linkedInUrl());
        return sb.toString();
    }

    private void appendContact(StringBuilder sb, String value) {
        if (hasText(value)) {
            if (!sb.isEmpty()) sb.append("  |  ");
            sb.append(value);
        }
    }

    // ── Summary ───────────────────────────────────────────────────────────────

    private void renderSummary(Document doc, String summary) throws DocumentException {
        if (!hasText(summary)) return;
        addSectionHeading(doc, "Summary");
        Paragraph p = body(summary);
        p.setSpacingAfter(4f);
        doc.add(p);
    }

    // ── Experience ────────────────────────────────────────────────────────────

    private void renderExperiences(Document doc, List<ResumeVersionData.ExperienceData> list)
            throws DocumentException {
        if (list == null || list.isEmpty()) return;
        addSectionHeading(doc, "Experience");
        for (ResumeVersionData.ExperienceData exp : list) {
            renderExperienceEntry(doc, exp);
        }
    }

    private void renderExperienceEntry(Document doc, ResumeVersionData.ExperienceData exp)
            throws DocumentException {
        // Role — Company · Location
        Paragraph roleLine = new Paragraph(LEADING, "", FONT_BOLD);
        if (hasText(exp.jobTitle())) {
            roleLine.add(new Chunk(exp.jobTitle(), FONT_BOLD));
        }
        if (hasText(exp.companyName())) {
            String sep = hasText(exp.jobTitle()) ? "  \u2014  " : "";
            roleLine.add(new Chunk(sep + exp.companyName(), FONT_BODY));
        }
        if (hasText(exp.location())) {
            roleLine.add(new Chunk("  \u00b7  " + exp.location(), FONT_ITALIC));
        }
        doc.add(roleLine);

        // Date range
        String dateRange = buildDateRange(exp.startDate(), exp.endDate(), exp.currentlyWorking());
        if (!dateRange.isEmpty()) {
            Paragraph dateLine = new Paragraph(LEADING, dateRange, FONT_ITALIC);
            dateLine.setSpacingAfter(2f);
            doc.add(dateLine);
        }

        // Description
        if (hasText(exp.description())) {
            Paragraph desc = body(exp.description());
            desc.setSpacingAfter(8f);
            doc.add(desc);
        } else {
            doc.add(spacer(6f));
        }
    }

    // ── Education ─────────────────────────────────────────────────────────────

    private void renderEducations(Document doc, List<ResumeVersionData.EducationData> list)
            throws DocumentException {
        if (list == null || list.isEmpty()) return;
        addSectionHeading(doc, "Education");
        for (ResumeVersionData.EducationData edu : list) {
            renderEducationEntry(doc, edu);
        }
    }

    private void renderEducationEntry(Document doc, ResumeVersionData.EducationData edu)
            throws DocumentException {
        // Degree, Field of Study
        String degreeLine = joinNonBlank(", ", edu.degree(), edu.fieldOfStudy());
        if (!degreeLine.isEmpty()) {
            doc.add(new Paragraph(LEADING, degreeLine, FONT_BOLD));
        }

        // Institution · Location
        String instLine = joinNonBlank("  \u00b7  ", edu.institutionName(), edu.location());
        if (!instLine.isEmpty()) {
            doc.add(new Paragraph(LEADING, instLine, FONT_BODY));
        }

        // Date range · Grade
        String dateRange = buildDateRange(edu.startDate(), edu.endDate(), false);
        String gradePart = hasText(edu.grade()) ? "  \u00b7  " + edu.grade() : "";
        String dateLine = dateRange + gradePart;
        if (!dateLine.isEmpty()) {
            Paragraph dp = new Paragraph(LEADING, dateLine, FONT_ITALIC);
            dp.setSpacingAfter(8f);
            doc.add(dp);
        } else {
            doc.add(spacer(6f));
        }
    }

    // ── Skills ────────────────────────────────────────────────────────────────

    private void renderSkills(Document doc, List<ResumeVersionData.SkillData> list)
            throws DocumentException {
        if (list == null || list.isEmpty()) return;
        addSectionHeading(doc, "Skills");

        StringBuilder sb = new StringBuilder();
        for (ResumeVersionData.SkillData s : list) {
            if (hasText(s.name())) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(s.name());
            }
        }
        if (!sb.isEmpty()) {
            Paragraph p = body(sb.toString());
            p.setSpacingAfter(4f);
            doc.add(p);
        }
    }

    // ── Section heading: UPPERCASE label + thin rule ──────────────────────────

    private void addSectionHeading(Document doc, String label) throws DocumentException {
        Paragraph heading = new Paragraph(LEADING, label.toUpperCase(), FONT_SECTION);
        heading.setSpacingBefore(10f);
        heading.setSpacingAfter(1f);
        doc.add(heading);

        Paragraph rule = new Paragraph();
        rule.add(new Chunk(new LineSeparator(0.5f, 100f, GRAY_RULE, Element.ALIGN_LEFT, -1f)));
        rule.setSpacingAfter(4f);
        doc.add(rule);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Paragraph body(String text) {
        Paragraph p = new Paragraph(LEADING, text, FONT_BODY);
        p.setAlignment(Element.ALIGN_LEFT);
        return p;
    }

    private Paragraph spacer(float spacingAfter) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(spacingAfter);
        return p;
    }

    private String buildDateRange(String start, String end, boolean current) {
        if (!hasText(start)) return "";
        if (current) return start + " \u2013 Present";
        if (hasText(end)) return start + " \u2013 " + end;
        return start;
    }

    private String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (hasText(p)) {
                if (!sb.isEmpty()) sb.append(sep);
                sb.append(p);
            }
        }
        return sb.toString();
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
