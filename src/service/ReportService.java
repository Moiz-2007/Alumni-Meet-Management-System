package service;

import dao.ReportDAO;
import util.Sql;

import java.util.List;

public class ReportService {
    private final ReportDAO dao = new ReportDAO();

    public List<ReportDAO.Report> reports() { return ReportDAO.REPORTS; }

    public ReportDAO.QueryResult run(ReportDAO.Report report) { return Sql.call(() -> dao.run(report.sql())); }
}
