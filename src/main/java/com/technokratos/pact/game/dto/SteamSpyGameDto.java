package com.technokratos.pact.game.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamSpyGameDto {
    private String name;
    private String developer;
    private String publisher;
    private int positive;
    private int negative;
    private String owners;
    private int median_2weeks;   //минуты
    private int price;
    private int average_2weeks;   //минуты
}