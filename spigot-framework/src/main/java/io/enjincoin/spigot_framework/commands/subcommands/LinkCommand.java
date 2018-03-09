package io.enjincoin.spigot_framework.commands.subcommands;

import io.enjincoin.sdk.client.Client;
import io.enjincoin.sdk.client.service.identities.IdentitiesService;
import io.enjincoin.sdk.client.service.identities.vo.CreateIdentityRequestBody;
import io.enjincoin.sdk.client.service.identities.vo.CreateIdentityResponseBody;
import io.enjincoin.sdk.client.service.identities.vo.Identity;
import io.enjincoin.sdk.client.service.identities.vo.IdentityField;
import io.enjincoin.spigot_framework.BasePlugin;
import io.enjincoin.spigot_framework.Bootstrap;
import io.enjincoin.spigot_framework.controllers.SdkClientController;
import io.enjincoin.spigot_framework.util.MessageUtil;
import io.enjincoin.spigot_framework.util.UuidUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class LinkCommand {

    private BasePlugin main;

    public LinkCommand(BasePlugin main) {
        this.main = main;
    }

    public void execute(CommandSender sender, String[] args) {
        UUID uuid = null;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            uuid = player.getUniqueId();
        } else {
            if (args.length >= 1) {
                try {
                    uuid = UuidUtil.stringToUuid(args[0]);
                } catch (IllegalArgumentException e) {
                    errorInvalidUuid(sender);
                }
            } else {
                final TextComponent text = TextComponent.of("UUID argument required.")
                        .color(TextColor.RED);
                MessageUtil.sendMessage(sender, text);
            }
        }

        if (uuid != null) {
            linkIdentity(sender, uuid);
        } else {
            errorInvalidUuid(sender);
        }
    }

    private void linkIdentity(CommandSender sender, UUID uuid) {
        Bootstrap bootstrap = this.main.getBootstrap();
        SdkClientController controller = bootstrap.getSdkController();
        Client client = controller.getClient();
        IdentitiesService service = client.getIdentitiesService();

        service.getIdentitiesAsync(new HashMap<String, Object>() {{
            put("uuid", uuid);
        }}, new FetchIdentityCallback(sender, uuid));
    }

    private void errorInvalidUuid(CommandSender sender) {
        final TextComponent text = TextComponent.of("The UUID provided is invalid.")
                .color(TextColor.RED);
        MessageUtil.sendMessage(sender, text);
    }

    private void errorRequestingIdentities(CommandSender sender, Throwable t) {
        final TextComponent text = TextComponent.of("An error occurred while requesting a player identity.")
                .color(TextColor.RED);
        this.main.getLogger().log(Level.WARNING, t.getMessage(), t);
        MessageUtil.sendMessage(sender, text);
    }

    private void errorCreatingIdentity(CommandSender sender, Throwable t) {
        final TextComponent text = TextComponent.of("An error occurred while creating a player identity.");
        this.main.getLogger().log(Level.WARNING, t.getMessage(), t);
        MessageUtil.sendMessage(sender, text);
    }

    private void errorLinkAlreadyExists(CommandSender sender, UUID uuid) {
        final TextComponent text = TextComponent.of("An identity has already been linked to ")
                .color(TextColor.RED)
                .append(TextComponent.of(uuid.toString())
                        .color(TextColor.GOLD));
        MessageUtil.sendMessage(sender, text);
    }

    private void handleCode(CommandSender sender, String code) {
        if (code == null || code.isEmpty()) {
            final TextComponent text = TextComponent.of("Could not acquire a player identity code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of("code not present.")
                            .color(TextColor.GOLD));
            MessageUtil.sendMessage(sender, text);
        } else {
            final TextComponent text = TextComponent.of("Identity Code: ")
                    .color(TextColor.GREEN)
                    .append(TextComponent.of(code)
                            .color(TextColor.GOLD));
            MessageUtil.sendMessage(sender, text);
        }
    }

    public abstract class CallbackBase<T> implements Callback<T> {

        private IdentitiesService service;
        private CommandSender sender;
        private UUID uuid;

        public CallbackBase(CommandSender sender, UUID uuid) {
            this.service = LinkCommand.this.main.getBootstrap().getSdkController().getClient().getIdentitiesService();
            this.sender = sender;
            this.uuid = uuid;
        }

        public IdentitiesService getService() {
            return service;
        }

        public CommandSender getSender() {
            return sender;
        }

        public UUID getUuid() {
            return uuid;
        }
    }

    public class FetchIdentityCallback extends CallbackBase<Identity[]> {

        public FetchIdentityCallback(CommandSender sender, UUID uuid) {
            super(sender, uuid);
        }

        @Override
        public void onResponse(Call<Identity[]> call, Response<Identity[]> response) {
            if (response.isSuccessful()) {
                Identity[] identities = response.body();
                if (identities.length == 0) {
                    // TODO: App ID needs to be configurable or acquired by some means.
                    getService().createIdentityAsync(
                            new CreateIdentityRequestBody(2, new IdentityField[]{
                                    new IdentityField("uuid", getUuid().toString())
                            }),
                            new CreateIdentityCallback(getSender(), getUuid()));
                } else {
                    Identity identity = identities[0];
                    String code = identity.getLinkingCode();
                    if (code == null || code.isEmpty())
                        errorLinkAlreadyExists(getSender(), getUuid());
                    else
                        handleCode(getSender(), code);
                }
            } else {
                try {
                    main.getLogger().warning(response.errorBody().string());
                } catch (IOException e) {
                    main.getLogger().warning("Unable to convert response error body to a string.");
                }
            }
        }

        @Override
        public void onFailure(Call<Identity[]> call, Throwable t) {
            errorRequestingIdentities(getSender(), t);
        }
    }

    public class CreateIdentityCallback extends CallbackBase<CreateIdentityResponseBody> {

        public CreateIdentityCallback(CommandSender sender, UUID uuid) {
            super(sender, uuid);
        }

        @Override
        public void onResponse(Call<CreateIdentityResponseBody> call, Response<CreateIdentityResponseBody> response) {
            if (response.isSuccessful()) {
                if (getSender() instanceof Player && !((Player) getSender()).isOnline())
                    return;

                String code = response.body().getLinkingCode();
                handleCode(getSender(), code);
            } else {
                try {
                    main.getLogger().warning(response.errorBody().string());
                } catch (IOException e) {
                    main.getLogger().warning("Unable to convert response error body to a string.");
                }
            }
        }

        @Override
        public void onFailure(Call<CreateIdentityResponseBody> call, Throwable t) {
            errorCreatingIdentity(getSender(), t);
        }
    }

}
