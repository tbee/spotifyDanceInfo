package org.tbee.spotifyDanceInfoWeb;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.tbee.spotifyDanceInfo.Cfg;

@Controller
public class IndexController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("ClientIdConnectForm", new ClientIdConnectForm());

        Cfg cfg = new Cfg();
        RefreshTokenConnectForm refreshTokenConnectForm = new RefreshTokenConnectForm();
        if (cfg.webapiRefreshToken() != null) {
            refreshTokenConnectForm.setRefreshToken(cfg.webapiRefreshToken());
        }
        model.addAttribute("RefreshTokenConnectForm", refreshTokenConnectForm);
        return "index";
    }

    @PostMapping
    public String indexSubmit(Model model, @ModelAttribute ClientIdConnectForm clientIdConnectForm, @ModelAttribute RefreshTokenConnectForm refreshTokenConnectForm) {
        return "redirect:/";
    }

    public static class ClientIdConnectForm {
        private String clientId;
        private String clientSecret;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    public static class RefreshTokenConnectForm {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}
