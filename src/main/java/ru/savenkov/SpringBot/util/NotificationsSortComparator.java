package ru.savenkov.SpringBot.util;

import org.springframework.stereotype.Component;
import ru.savenkov.SpringBot.model.NotificationInfo;

import java.util.Comparator;


public class NotificationsSortComparator implements Comparator<NotificationInfo> {


    @Override
    public int compare(NotificationInfo o1, NotificationInfo o2) {
        if(o1.getDayOfWeek().getValue() > o2.getDayOfWeek().getValue()){
            return 1;
        }
        else if(o1.getDayOfWeek().getValue() < o2.getDayOfWeek().getValue()){
            return -1;
        }

        //сюда доходят оповещения с одинаковыми днями
        return o1.getNotificationTime().compareTo(o2.getNotificationTime());
    }
}
