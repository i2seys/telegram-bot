package ru.savenkov.SpringBot.util;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

@Component
public class DayOfWeekFromEnglishToRussian {
    public DayOfWeekFromEnglishToRussian() {
    }
    public String parse(DayOfWeek day){
        switch(day){
            case MONDAY -> {return "Понедельник";}
            case TUESDAY -> {return "Вторник";}
            case WEDNESDAY -> {return "Среда";}
            case THURSDAY -> {return "Четверг";}
            case FRIDAY -> {return "Пятница";}
            case SATURDAY -> {return "Суббота";}
            case SUNDAY -> {return "Воскресенье";}
            default -> {
                throw new RuntimeException("");
            }
        }
    }
}
