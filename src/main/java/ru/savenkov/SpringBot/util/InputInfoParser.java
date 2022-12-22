package ru.savenkov.SpringBot.util;


import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Component
public class InputInfoParser {
    public InputInfoParser() {
    }
    public LocalTime parseTime(String time){
        //если строка вида 9:20 | 5:20
        int hours, minutes;
        if(time.length() == 4){
            hours = Integer.parseInt(time.substring(0, 1));
            minutes = Integer.parseInt(time.substring(2,4));
        }
        //если строка вида 02:20 | 17:50
        else{
            hours = Integer.parseInt(time.substring(0, 2));
            minutes = Integer.parseInt(time.substring(3,5));
        }
        return LocalTime.of(hours, minutes);
    }
    public DayOfWeek parseDayOfWeek(String dayOfWeek){
        return switch (dayOfWeek.toLowerCase()) {
            case "понедельник" -> DayOfWeek.MONDAY;
            case "вторник" -> DayOfWeek.TUESDAY;
            case "среда" -> DayOfWeek.WEDNESDAY;
            case "четверг" -> DayOfWeek.THURSDAY;
            case "пятница" -> DayOfWeek.FRIDAY;
            case "суббота" -> DayOfWeek.SATURDAY;
            case "воскресенье" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }
}
