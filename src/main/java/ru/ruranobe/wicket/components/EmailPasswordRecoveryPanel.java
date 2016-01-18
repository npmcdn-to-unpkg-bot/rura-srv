package ru.ruranobe.wicket.components;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.util.string.Strings;
import ru.ruranobe.misc.Token;
import ru.ruranobe.misc.smtp.Email;
import ru.ruranobe.mybatis.MybatisUtil;
import ru.ruranobe.mybatis.entities.tables.User;
import ru.ruranobe.mybatis.mappers.UsersMapper;
import ru.ruranobe.mybatis.mappers.cacheable.CachingFacade;

import javax.mail.MessagingException;

public class EmailPasswordRecoveryPanel extends Panel
{
    private static final String EMAIL_PASSWORD_RECOVERY_FORM = "emailPasswordRecoveryForm";
    private static final long EXPIRATION_TIME_6_HOURS = 21600000L;
    private static final long serialVersionUID = 1L;
    private String emailOrLogin;

    public EmailPasswordRecoveryPanel(String id)
    {
        super(id);
        add(new FeedbackPanel("feedback"));
        add(new EmailPasswordRecoveryForm(EMAIL_PASSWORD_RECOVERY_FORM));
    }

    public String getEmailOrLogin()
    {
        return emailOrLogin;
    }

    public void setEmailOrLogin(String emailOrLogin)
    {
        this.emailOrLogin = emailOrLogin;
    }

    public final class EmailPasswordRecoveryForm extends StatelessForm<EmailPasswordRecoveryPanel>
    {
        private static final long serialVersionUID = 1L;

        public EmailPasswordRecoveryForm(String id)
        {
            super(id);
            this.setOutputMarkupId(true);
            this.setMarkupId("settingsPass");
            setModel(new CompoundPropertyModel<>(EmailPasswordRecoveryPanel.this));
            add(new TextField<String>("emailOrLogin"));
        }

        @Override
        public final void onSubmit()
        {
            if (Strings.isEmpty(emailOrLogin))
            {
                error("Укажите, пожалуйста, электронный адрес или логин.");
            }
            else if (emailOrLogin.length() > 255)
            {
                error("Длина электронного адреса или логина не должна превышать 255 символов.");
            }
            else
            {
                try (SqlSession session = MybatisUtil.getSessionFactory().openSession())
                {
                    UsersMapper usersMapper = CachingFacade.getCacheableMapper(session, UsersMapper.class);
                    User user = usersMapper.getUserByEmail(emailOrLogin);
                    if (user == null)
                    {
                        user = usersMapper.getUserByUsername(emailOrLogin);
                    }
                    if (user == null)
                    {
                        error("Пользователя с таким логином или электронным адресом не существует.");
                    }
                    else
                    {
                        if (!user.isEmailActivated())
                        {
                            error("Электронный адрес пользователя не был подтвержден.");
                        }
                        else if (user.getPassRecoveryToken() != null
                                && user.getPassRecoveryTokenDate().getTime() > System.currentTimeMillis())
                        {
                            error("На указанный электронной адрес уже было отправлено письмо.");
                        }
                        else
                        {
                            Token token = Token.valueOf(user.getUserId(), EXPIRATION_TIME_6_HOURS);
                            user.setPassRecoveryToken(token.getTokenValue());
                            user.setPassRecoveryTokenDate(token.getTokenExpirationDate());
                            usersMapper.updateUser(user);
                            try
                            {
                                Email.sendPasswordRecoveryMessage(user.getEmail(), user.getPassRecoveryToken());
                                session.commit();
                            }
                            catch (MessagingException ex)
                            {
                                error("Отправка сообщения на указанный электронный адрес не удалась. Свяжитесь, пожалуйста, с администрацией сайта.");
                            }
                        }
                    }
                }
            }
        }
    }
}
