package bode.pki.authorization.system.verticles;

import bode.pki.authorization.system.services.authorization.AuthorizationService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandRegistry;
import io.vertx.ext.shell.term.HttpTermOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ShellVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellVerticle.class);

    private AuthorizationService authorizationService;

    @Override
    public void start(Promise<Void> startPromise) {
        authorizationService =
                AuthorizationService.createProxy(vertx, AuthorizationService.SERVICE_ADDRESS);

        ShellServiceOptions shellServiceOptions =
                new ShellServiceOptions().setWelcomeMessage("\n  Welcome to opendata-blockchain-na CLI!\n\n");

        int cli_port = config().getInteger("CLI_PORT", 8082);

        ShellService shellService = ShellService.create(vertx,
                shellServiceOptions.setHttpOptions(
                        new HttpTermOptions()
                                .setHost("0.0.0.0")
                                .setPort(cli_port)
                )
        );

        shellService.start(handler -> {
            if (handler.succeeded()) {
                LOGGER.info("[SHELL] Successfully launched cli on port [{}]", cli_port);
                startPromise.complete();
            } else {
                LOGGER.error("[SHELL] Failed to start server at [{}]: {}", cli_port, handler.cause());
                startPromise.fail(handler.cause());
            }
        });

        /*CommandBuilder onboard = CommandBuilder.command("onboard");
        onboard.processHandler(process -> {
            authorizationDatabaseService.onboard();
            process.write("Triggered onboarding process\n");
            process.end();
        });

        CommandBuilder listAllowed = CommandBuilder.command("list-allowed");
        listAllowed.processHandler(process ->
                authorizationDatabaseService.getAllowed(ar -> {
                    if (ar.succeeded()) {
                        process.write(ar.result() + "\n");
                    } else {
                        process.write(ar.cause().getMessage() + "\n");
                    }
                    process.end();
                })
        );

        CommandBuilder allow = CommandBuilder.command("allow");
        allow.processHandler(process -> {
            List<String> args = process.args();
            if (args.size() != 4) {
                process.write("allow: try \'allow private_key host nodePort adPort\'\n");
                process.end();
            } else {
                try {
                    String key = args.get(0);
                    String host = args.get(1);
                    int nodePort = Integer.parseInt(args.get(2));
                    int adPort = Integer.parseInt(args.get(3));
                    authorizationDatabaseService.allow(key, host, nodePort, adPort, ar -> {
                        if (ar.succeeded()) {
                            process.write(ar.result() + "\n");
                        } else {
                            process.write(ar.cause().getMessage() + "\n");
                        }
                        process.end();
                    });
                } catch (NumberFormatException e) {
                    process.write("allow: try \'allow private_key host nodePort adPort\'\n");
                    process.end();
                }
            }
        });

        CommandBuilder refuse = CommandBuilder.command("refuse");
        refuse.processHandler(process -> {
            List<String> args = process.args();
            if (args.size() != 1) {
                process.write("refuse: try \'refuse uuid\'\n");
                process.end();
            } else {
                try {
                    String uuid = args.get(0);
                    authorizationDatabaseService.refuse(uuid, ar -> {
                        if (ar.succeeded()) {
                            process.write(ar.result() + "\n");
                        } else {
                            process.write(ar.cause().getMessage() + "\n");
                        }
                        process.end();
                    });
                } catch (NumberFormatException e) {
                    process.write("refuse: try \'refuse hashcode\'\n");
                    process.end();
                }
            }
        });

        CommandBuilder revoke = CommandBuilder.command("revoke");
        revoke.processHandler(process -> {
            List<String> args = process.args();
            if (args.size() != 1) {
                process.write("revoke: try \'revoke hash\'\n");
                process.end();
            } else {
                authorizationDatabaseService.revoke(args.get(0), ar -> {
                    if (ar.succeeded()) {
                        process.write(ar.result() + "\n");
                    } else {
                        process.write(ar.cause().getMessage() + "\n");
                    }
                    process.end();
                });
            }
        });*/

        CommandBuilder sign = CommandBuilder.command("sign");
        sign.processHandler(process -> {
            List<String> args = process.args();
            if (args.size() != 2) {
                process.write("sign: try \'sign message privateKey\'\n");
                process.end();
            } else {
                authorizationService.sign(args.get(0), args.get(1), ar -> {
                    if (ar.succeeded()) {
                        process.write(ar.result() + "\n");
                    } else {
                        process.write(ar.cause().getMessage() + "\n");
                    }
                    process.end();
                });
            }
        });

        CommandBuilder verify = CommandBuilder.command("verify");
        verify.processHandler(process -> {
            List<String> args = process.args();
            if (args.size() != 3) {
                process.write("sign: try \'verify message signature publicKey\'\n");
                process.end();
            } else {
                authorizationService.verify(args.get(0), args.get(1), args.get(2), ar -> {
                    if (ar.succeeded()) {
                        process.write(ar.result() + "\n");
                    } else {
                        process.write(ar.cause().getMessage() + "\n");
                    }
                    process.end();
                });
            }
        });

        CommandRegistry registry = CommandRegistry.getShared(vertx);
        /*registry.registerCommand(onboard.build(vertx));
        registry.registerCommand(listAllowed.build(vertx));
        registry.registerCommand(allow.build(vertx));
        registry.registerCommand(refuse.build(vertx));
        registry.registerCommand(revoke.build(vertx));*/
        registry.registerCommand(sign.build(vertx));
        registry.registerCommand(verify.build(vertx));
    }
}
