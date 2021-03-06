package ru.ruranobe.wicket.components;

import org.apache.wicket.authentication.IAuthenticationStrategy;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import ru.ruranobe.misc.MD5;
import ru.ruranobe.wicket.LoginSession;

public class LoginPanel extends Panel
{
    public LoginPanel(final String id)
    {
        super(id);
        add(new FeedbackPanel("feedback").setFilter(new ContainerFeedbackMessageFilter(this)));
        add(new LoginForm(LOGIN_FORM));
    }

    protected LoginForm getForm()
    {
        return (LoginForm) get(LOGIN_FORM);
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(final String password)
    {
        this.password = MD5.crypt(password);
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }

    public boolean getRememberMe()
    {
        return rememberMe;
    }

    public void setRememberMe(final boolean rememberMe)
    {
        this.rememberMe = rememberMe;
    }

    private boolean login(String username, String password)
    {
        return LoginSession.get().signIn(username, password);
    }

    private boolean isLoggedIn()
    {
        return LoginSession.get().isSignedIn();
    }

    protected void onLoginFailed()
    {
        error("Не удалось распознать введенные данные. ");
    }

    protected void onLoginSucceeded()
    {
        // logon successful. Continue to the original destination
        continueToOriginalDestination();

        // If we get this far, it means that we should redirect to the home page
        throw new RedirectToUrlException(urlFor(getPage().getClass(), getPage().getPageParameters()).toString());
        //setResponsePage(getApplication().getHomePage());
    }

    public final class LoginForm extends StatelessForm<LoginPanel>
    {
        public LoginForm(String id)
        {
            super(id);

            setModel(new CompoundPropertyModel<>(LoginPanel.this));

            // Attach textfields for username and password
            add(new TextField<String>("username"));
            add(new PasswordTextField("password"));
            add(new CheckBox("rememberMe"));
        }

        @Override
        public final void onSubmit()
        {
            IAuthenticationStrategy strategy = getApplication().getSecuritySettings()
                                                               .getAuthenticationStrategy();

            if (login(getUsername(), getPassword()))
            {
                if (rememberMe)
                {
                    strategy.save(username, password);
                }
                else
                {
                    strategy.remove();
                }

                onLoginSucceeded();
            }
            else
            {
                onLoginFailed();
                strategy.remove();
            }
        }

        private static final long serialVersionUID = 1L;
    }

    private boolean rememberMe = true;
    private String password;
    private String username;
    private static final String LOGIN_FORM = "loginForm";
    private static final long serialVersionUID = 1L;
}
