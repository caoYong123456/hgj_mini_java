//package com.ej.hgj.task;
//
//import com.ej.hgj.dao.cron.CronDaoMapper;
//import com.ej.hgj.entity.cron.Cron;
//import com.ej.hgj.task.repair.service.RepairTaskService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.SchedulingConfigurer;
//import org.springframework.scheduling.config.ScheduledTaskRegistrar;
//import org.springframework.scheduling.support.CronTrigger;
//
//@Configuration
//@EnableScheduling
//public class RepairWoStaFinishTask implements SchedulingConfigurer {
//
//    Logger logger = LoggerFactory.getLogger(getClass());
//
//    @Autowired
//    private CronDaoMapper cronDaoMapper;
//
//    @Autowired
//    private RepairTaskService repairTaskService;
//
//    /**
//     * 已完工工单定时任务
//     */
//    @Override
//    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
//        scheduledTaskRegistrar.addTriggerTask(() -> repairTaskService.repairWoStaFinishUpdate(), triggerContext -> {
//            Cron cronByType = cronDaoMapper.getByType("3");
//            String cron = cronByType.getCron();
//            return new CronTrigger(cron).nextExecutionTime(triggerContext);
//        });
//    }
//}
