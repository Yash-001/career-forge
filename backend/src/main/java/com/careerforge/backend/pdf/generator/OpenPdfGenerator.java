package com.careerforge.backend.pdf.generator;

import com.careerforge.backend.pdf.dto.ResumeVersionData;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Component
public class OpenPdfGenerator implements PdfGenerator {

    private static final Font NAME_FONT    = new Font(Font.HELVETICA, 18, Font.BOLD,  Color.BLACK);
    private static final Font CONTACT_FONT = new Font(Font.HELVETICA,  9, Font.NORMAL, Color.DARK_GRAY);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 11, Font.BOLD,  Color.BLACK);
    private static final Font BODY_FONT    = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font BOLD_FONT    = new Font(Font.HELVETICA, 10, Font.BOLD,  Color.BLACK);
    private static final Font ITALIC_FONT  = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.DARK_GRAY);

    @Override
    public byte[] generate(ResumeVersionData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER, 54, 54, 54, 54);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, data);
            addSummary(doc, data.professionalSummary());
            addExperiences(doc, data.experiences());
            addEducations(doc, data.educations());
            addSkills(doc, data.skills());

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addHeader(Document doc, ResumeVersionData data) throws DocumentException {
        if (data.fullName() != null && !data.fullName().isBlank()) {
            doc.add(new Paragraph(data.fullName(), NAME_FONT));
        }

        StringBuilder contact = new StringBuilder();
        appendIfPresent(contact, data.email());
        appendIfPresent(contact, data.phone());
        appendIfPresent(contact, data.location());
        appendIfPresent(contact, data.linkedInUrl());

        if (!contact.isEmpty()) {
            doc.add(new Paragraph(contact.toString().trim(), CONTACT_FONT));
        }
        doc.add(Chunk.NEWLINE);
    }

    private void addSummary(Document doc, String summary) throws DocumentException {
        if (summary == null || summary.isBlank()) return;
        addSectionHeading(doc, "Summary");
        doc.add(new Paragraph(summary, BODY_FONT));
        doc.add(Chunk.NEWLINE);
    }

    private void addExperiences(Document doc, List<ResumeVersionData.ExperienceData> experiences) throws DocumentException {
        if (experiences == null || experiences.isEmpty()) return;
        addSectionHeading(doc, "Experience");
        for (ResumeVersionData.ExperienceData exp : experiences) {
            Paragraph title = new Paragraph();
            title.add(new Chunk(exp.jobTitle() != null ? exp.jobTitle() : "", BOLD_FONT));
            title.add(new Chunk("  —  " + (exp.companyName() != null ? exp.companyName() : ""), BODY_FONT));
            doc.add(title);

            String dateRange = buildDateRange(exp.startDate(), exp.endDate(), exp.currentlyWorking());
            if (!dateRange.isBlank()) {
                doc.add(new Paragraph(dateRange, ITALIC_FONT));
            }
            if (exp.description() != null && !exp.description().isBlank()) {
                doc.add(new Paragraph(exp.description(), BODY_FONT));
            }
            doc.add(Chunk.NEWLINE);
        }
    }

    private void addEducations(Document doc, List<ResumeVersionData.EducationData> educations) throws DocumentException {
        if (educations == null || educations.isEmpty()) return;
        addSectionHeading(doc, "Education");
        for (ResumeVersionData.EducationData edu : educations) {
            String degreeLine = joinNonBlank(", ", edu.degree(), edu.fieldOfStudy());
            if (!degreeLine.isBlank()) {
                doc.add(new Paragraph(degreeLine, BOLD_FONT));
            }
            if (edu.institutionName() != null && !edu.institutionName().isBlank()) {
                doc.add(new Paragraph(edu.institutionName(), BODY_FONT));
            }
            String dateRange = buildDateRange(edu.startDate(), edu.endDate(), false);
            if (!dateRange.isBlank()) {
                doc.add(new Paragraph(dateRange, ITALIC_FONT));
            }
            doc.add(Chunk.NEWLINE);
        }
    }

    private void addSkills(Document doc, List<ResumeVersionData.SkillData> skills) throws DocumentException {
        if (skills == null || skills.isEmpty()) return;
        addSectionHeading(doc, "Skills");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < skills.size(); i++) {
            sb.append(skills.get(i).name());
            if (i < skills.size() - 1) sb.append(", ");
        }
        doc.add(new Paragraph(sb.toString(), BODY_FONT));
    }

    private void addSectionHeading(Document doc, String text) throws DocumentException {
        Paragraph heading = new Paragraph(text.toUpperCase(), SECTION_FONT);
        doc.add(heading);
        doc.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(0.5f, 100, Color.GRAY, Element.ALIGN_LEFT, -2)));
        doc.add(Chunk.NEWLINE);
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (!sb.isEmpty()) sb.append("  |  ");
            sb.append(value);
        }
    }

    private String buildDateRange(String start, String end, boolean current) {
        if (start == null || start.isBlank()) return "";
        return current ? start + " – Present" : (end != null && !end.isBlank() ? start + " – " + end : start);
    }

    private String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) sb.append(sep);
                sb.append(p);
            }
        }
        return sb.toString();
    }
}
