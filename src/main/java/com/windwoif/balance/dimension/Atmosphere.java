package com.windwoif.balance.dimension;

import com.windwoif.balance.Chemical;

import java.util.HashMap;

public record Atmosphere(int temperature, HashMap<Chemical, Integer> atmosphere) {}
