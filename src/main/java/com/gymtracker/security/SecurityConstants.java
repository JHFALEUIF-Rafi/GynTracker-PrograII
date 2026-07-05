package com.gymtracker.security;

/**
 * Security constants for public routes and role authorities.
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String ROOT_ROUTE = "/";
    public static final String LOGIN_ROUTE = "/login";
    public static final String ERROR_ROUTE = "/error";
    public static final String STATIC_ROUTE_PATTERN = "/static/**";
    public static final String DASHBOARD_ROUTE = "dashboard";
    public static final String ATHLETE_PROFILE_ROUTE = "athletes/profile";
    public static final String WORKOUT_HISTORY_ROUTE = "workouts/history";

    public static final String[] PUBLIC_ROUTES = {
            ROOT_ROUTE,
            LOGIN_ROUTE,
            ERROR_ROUTE,
            STATIC_ROUTE_PATTERN
    };

    public static final String ROLE_ATHLETE = "ROLE_ATHLETE";
    public static final String ROLE_COACH = "ROLE_COACH";
    public static final String ROLE_NUTRITIONIST = "ROLE_NUTRITIONIST";
}
