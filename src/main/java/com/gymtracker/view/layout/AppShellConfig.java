package com.gymtracker.view.layout;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

/**
 * Application shell configuration entry point. Vaadin requires app-shell
 * annotations (such as {@code @Theme}) to live on a single class implementing
 * {@link AppShellConfigurator} rather than on an arbitrary bean.
 */
@Theme("lumo")
public class AppShellConfig implements AppShellConfigurator {
}
