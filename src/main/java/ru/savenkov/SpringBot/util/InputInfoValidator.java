package ru.savenkov.SpringBot.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class InputInfoValidator {

    public InputInfoValidator() {
    }

    public boolean timeIsValid(String time){
        if(time == null || time.isEmpty()){
            return false;
        }
        Pattern pattern = Pattern.compile("\\d?\\d:\\d\\d");//проверка, подходит ли строка по шаблону 00:00 - 99:99 или 0:00-9:99
        Matcher matcher = pattern.matcher(time);
        if(!matcher.matches()){
            return false;
        }
        //случай, если время вида 12:33
        int hours, minutes;
        if(time.length() == 5) {
            hours = Integer.parseInt(time.substring(0, 2));
            minutes = Integer.parseInt(time.substring(3, 5));
        }
        //случай, если время вида 9:22
        else {
            hours = Integer.parseInt(time.substring(0, 1));
            minutes = Integer.parseInt(time.substring(2, 4));
        }
        if(hours < 24 && hours >= 0 && minutes < 60 && minutes >= 0){
            return true;
        }
        else{
            return false;
        }
    }
    public boolean notificationTextIsValid(String text){
        if(text == null || text.isEmpty() || text.length() > 255){
            return false;
        }
        else {
            return true;
        }
    }
    public boolean dayOfWeekIsValid(String dayOfWeek){
        if(dayOfWeek == null || dayOfWeek.isEmpty()){
            return false;
        }
        dayOfWeek = dayOfWeek.toLowerCase();
        List<String> daysOfWeek = Arrays.asList("понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье");
        if(daysOfWeek.contains(dayOfWeek)){
            return true;
        }
        else{
            return false;
        }
    }
    public boolean indexOfNotificationToDeleteIsValid(String index, int notificationsCount){
        if(index == null || index.isEmpty()){
            return false;
        }
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(index);
        if(!matcher.matches()){
            return false;
        }

        if(Integer.parseInt(index) <= notificationsCount && Integer.parseInt(index) >= 0){
            return true;
        }
        else{
            return false;
        }
    }
}
