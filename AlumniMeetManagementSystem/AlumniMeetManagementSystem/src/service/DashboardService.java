package service;

import dao.DashboardDAO;
import model.DashboardStats;
import util.Sql;

public class DashboardService {
    private final DashboardDAO dao = new DashboardDAO();
    public DashboardStats stats() { return Sql.call(dao::load); }
}
