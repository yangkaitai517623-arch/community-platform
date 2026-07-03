package com.community.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardStats {

    private long totalUsers;
    private long activeUsers;
    private long totalGoods;
    private long onSaleGoods;
    private long pendingRepairs;
    private long pendingErrands;
    private long activeRepairOrders;
    private long completedRepairOrders;
    private long reviewedRepairOrders;
    private long activeErrandOrders;
    private long completedErrandOrders;
    private long reviewedErrandOrders;
    private long totalPosts;
    private long unreadNotifications;

    // 今日数据
    private long todayOrders;
    private long todayNewUsers;
    private long todayNewPosts;
    private long monthOrders;
    private long urgentOrders;
    private double satisfactionRate;

    // 图表数据
    private List<String> trendDays;
    private List<Long> orderTrend;
    private List<Long> completedTrend;
    private List<BusinessDistributionItem> businessDistribution;

    @Data
    public static class BusinessDistributionItem {
        private String name;
        private long value;

        public BusinessDistributionItem(String name, long value) {
            this.name = name;
            this.value = value;
        }
    }
}
