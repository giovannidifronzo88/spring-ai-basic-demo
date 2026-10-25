package com.test.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTimeTool {

    @Tool(description = "Get the current date and time in ISO 8601 format for a given city or timezone")
    public String getCurrentDateTime(
            @ToolParam(description = "IANA timezone ID of the place, e.g. Europe/London, Asia/Yerevan, Europe/Rome") String timeZone) {
        try {
            return ZonedDateTime.now(ZoneId.of(timeZone)).toString();
        } catch (Exception e) {
            return "Fuso orario non valido: " + timeZone;
        }
    }
}
