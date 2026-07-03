package com.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.config.JwtConfig;
import com.community.dto.DashboardStats;
import com.community.dto.LoginRequest;
import com.community.dto.LoginResponse;
import com.community.dto.PageResult;
import com.community.dto.RegisterRequest;
import com.community.dto.Result;
import com.community.entity.ErrandRequest;
import com.community.entity.ForumPost;
import com.community.entity.GoodsOrder;
import com.community.entity.RepairRequest;
import com.community.entity.SysUser;
import com.community.repository.ErrandOrderMapper;
import com.community.repository.ErrandRequestMapper;
import com.community.repository.ForumPostMapper;
import com.community.repository.GoodsOrderMapper;
import com.community.repository.NotificationMapper;
import com.community.repository.RepairOrderMapper;
import com.community.repository.RepairRequestMapper;
import com.community.repository.SecondHandGoodsMapper;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SecondHandGoodsMapper goodsMapper;
    private final GoodsOrderMapper goodsOrderMapper;
    private final RepairRequestMapper repairRequestMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final ErrandRequestMapper errandRequestMapper;
    private final ErrandOrderMapper errandOrderMapper;
    private final ForumPostMapper forumPostMapper;
    private final NotificationMapper notificationMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new RuntimeException("登录参数不能为空");
        }
        SysUser user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtConfig.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user);
    }

    public Result<Void> register(RegisterRequest request) {
        if (request == null) {
            return Result.error("注册参数不能为空");
        }
        SysUser existing = userMapper.findByUsername(request.getUsername());
        if (existing != null) {
            return Result.error("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setBuilding(request.getBuilding());
        user.setRoom(request.getRoom());
        user.setRole(0);
        user.setStatus(1);
        userMapper.insert(user);
        return Result.success("注册成功", null);
    }

    public SysUser getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public Result<Void> updateUser(SysUser user) {
        if (user == null || user.getId() == null) {
            return Result.error("用户ID不能为空");
        }
        SysUser existing = userMapper.selectById(user.getId());
        if (existing == null) {
            return Result.error("用户不存在");
        }

        user.setPassword(null);
        user.setRole(null);
        user.setStatus(null);
        user.setDeleted(null);
        userMapper.updateById(user);
        return Result.success("更新成功", null);
    }

    public Result<Void> changePassword(Long userId, String oldPwd, String newPwd) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            return Result.error("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPwd));
        userMapper.updateById(user);
        return Result.success("密码修改成功", null);
    }

    public PageResult<SysUser> listUsers(int page, int size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, 0)
                .orderByDesc(SysUser::getCreatedAt);

        IPage<SysUser> pageResult = userMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    public Result<Void> updateStatus(Long userId, Integer status) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("用户状态不正确");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.success("状态更新成功", null);
    }

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        stats.setTotalUsers(userMapper.countUsers());
        stats.setActiveUsers(userMapper.countActiveUsers());
        stats.setTotalGoods(goodsMapper.selectCount(null));
        stats.setOnSaleGoods(goodsMapper.countOnSale());
        stats.setPendingRepairs(repairRequestMapper.countPending());
        stats.setPendingErrands(errandRequestMapper.countPending());

        stats.setActiveRepairOrders(repairOrderMapper.countByStatus(0));
        stats.setCompletedRepairOrders(repairOrderMapper.countByStatus(1));
        stats.setReviewedRepairOrders(repairOrderMapper.countByStatus(2));
        stats.setActiveErrandOrders(errandOrderMapper.countByStatus(0));
        stats.setCompletedErrandOrders(errandOrderMapper.countByStatus(1));
        stats.setReviewedErrandOrders(errandOrderMapper.countByStatus(2));

        LambdaQueryWrapper<ForumPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(ForumPost::getStatus, 1);
        stats.setTotalPosts(forumPostMapper.selectCount(postWrapper));
        stats.setUnreadNotifications(notificationMapper.selectCount(
                new LambdaQueryWrapper<com.community.entity.Notification>()
                        .eq(com.community.entity.Notification::getIsRead, 0)));

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);

        long todayOrders = countCreatedBetween(todayStart, todayEnd);
        long monthOrders = countCreatedBetween(monthStart, todayEnd);
        stats.setTodayOrders(todayOrders);
        stats.setMonthOrders(monthOrders);
        stats.setUrgentOrders(countUrgentPendingOrders());
        stats.setSatisfactionRate(calculateSatisfactionRate());

        LambdaQueryWrapper<SysUser> todayUserWrapper = new LambdaQueryWrapper<>();
        todayUserWrapper.between(SysUser::getCreatedAt, todayStart, todayEnd)
                .eq(SysUser::getRole, 0);
        stats.setTodayNewUsers(userMapper.selectCount(todayUserWrapper));

        LambdaQueryWrapper<ForumPost> todayPostWrapper = new LambdaQueryWrapper<>();
        todayPostWrapper.between(ForumPost::getCreatedAt, todayStart, todayEnd);
        stats.setTodayNewPosts(forumPostMapper.selectCount(todayPostWrapper));

        fillChartStats(stats);

        return stats;
    }

    private long countCreatedBetween(LocalDateTime start, LocalDateTime end) {
        long errandCount = errandRequestMapper.selectCount(new LambdaQueryWrapper<ErrandRequest>()
                .between(ErrandRequest::getCreatedAt, start, end));
        long repairCount = repairRequestMapper.selectCount(new LambdaQueryWrapper<RepairRequest>()
                .between(RepairRequest::getCreatedAt, start, end));
        long goodsOrderCount = goodsOrderMapper.selectCount(new LambdaQueryWrapper<GoodsOrder>()
                .between(GoodsOrder::getCreatedAt, start, end));
        return errandCount + repairCount + goodsOrderCount;
    }

    private long countCompletedBetween(LocalDateTime start, LocalDateTime end) {
        long errandCompleted = errandRequestMapper.selectCount(new LambdaQueryWrapper<ErrandRequest>()
                .eq(ErrandRequest::getStatus, 3)
                .between(ErrandRequest::getUpdatedAt, start, end));
        long repairCompleted = repairRequestMapper.selectCount(new LambdaQueryWrapper<RepairRequest>()
                .eq(RepairRequest::getStatus, 3)
                .between(RepairRequest::getUpdatedAt, start, end));
        long goodsCompleted = goodsOrderMapper.selectCount(new LambdaQueryWrapper<GoodsOrder>()
                .eq(GoodsOrder::getStatus, 2)
                .between(GoodsOrder::getUpdatedAt, start, end));
        return errandCompleted + repairCompleted + goodsCompleted;
    }

    private long countUrgentPendingOrders() {
        long urgentErrands = errandRequestMapper.selectCount(new LambdaQueryWrapper<ErrandRequest>()
                .eq(ErrandRequest::getStatus, 0)
                .eq(ErrandRequest::getUrgency, 1));
        long urgentRepairs = repairRequestMapper.selectCount(new LambdaQueryWrapper<RepairRequest>()
                .eq(RepairRequest::getStatus, 0)
                .eq(RepairRequest::getUrgency, 1));
        return urgentErrands + urgentRepairs;
    }

    private double calculateSatisfactionRate() {
        long reviewedOrders = repairOrderMapper.countByStatus(2) + errandOrderMapper.countByStatus(2);
        long completedOrders = repairOrderMapper.countByStatus(1)
                + repairOrderMapper.countByStatus(2)
                + errandOrderMapper.countByStatus(1)
                + errandOrderMapper.countByStatus(2);
        if (completedOrders == 0) {
            return 0;
        }
        return Math.round((reviewedOrders * 10000.0 / completedOrders)) / 100.0;
    }

    private void fillChartStats(DashboardStats stats) {
        List<String> days = new ArrayList<>();
        List<Long> orderTrend = new ArrayList<>();
        List<Long> completedTrend = new ArrayList<>();

        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = LocalDateTime.of(day, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(day, LocalTime.MAX);
            days.add(day.getMonthValue() + "/" + day.getDayOfMonth());
            orderTrend.add(countCreatedBetween(start, end));
            completedTrend.add(countCompletedBetween(start, end));
        }

        stats.setTrendDays(days);
        stats.setOrderTrend(orderTrend);
        stats.setCompletedTrend(completedTrend);

        List<DashboardStats.BusinessDistributionItem> distribution = new ArrayList<>();
        distribution.add(new DashboardStats.BusinessDistributionItem("跑腿", errandRequestMapper.selectCount(null)));
        distribution.add(new DashboardStats.BusinessDistributionItem("检修", repairRequestMapper.selectCount(null)));
        distribution.add(new DashboardStats.BusinessDistributionItem("二手", goodsOrderMapper.selectCount(null)));
        distribution.add(new DashboardStats.BusinessDistributionItem("论坛", forumPostMapper.selectCount(new LambdaQueryWrapper<ForumPost>()
                .eq(ForumPost::getStatus, 1))));
        stats.setBusinessDistribution(distribution);
    }
}
