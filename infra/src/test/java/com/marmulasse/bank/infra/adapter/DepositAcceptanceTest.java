package com.marmulasse.bank.infra.adapter;

import com.marmulasse.bank.account.aggregate.Account;
import com.marmulasse.bank.account.aggregate.Amount;
import com.marmulasse.bank.account.commands.MakeDepositCommand;
import com.marmulasse.bank.account.commands.handlers.CommandHandler;
import com.marmulasse.bank.account.commands.handlers.DepositCommandHandler;
import com.marmulasse.bank.account.port.AccountRepository;
import com.marmulasse.bank.infra.bus.CommandBus;
import com.marmulasse.bank.infra.bus.DomainBus;
import com.marmulasse.bank.infra.bus.QueryBus;
import com.marmulasse.bank.query.account.AccountEventHandler;
import com.marmulasse.bank.query.account.balance.AccountId;
import com.marmulasse.bank.query.account.balance.Balance;
import com.marmulasse.bank.query.account.port.QueryHandler;
import com.marmulasse.bank.query.account.queries.GetBalanceFromAccountId;
import com.marmulasse.bank.query.account.queries.Result;
import com.marmulasse.bank.query.account.queries.handlers.GetBalanceByIdQueryHandler;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class DepositAcceptanceTest {

    private CommandBus commandBus;
    private AccountRepository accountRepository;
    private QueryBus queryBus;
    private InMemoryBalanceRepository balanceRepository;

    @Before
    public void setUp() throws Exception {
        balanceRepository = new InMemoryBalanceRepository(new HashMap<>());
        AccountEventHandler accountEventHandler = new AccountEventHandler(balanceRepository);

        DomainBus domainBus = new DomainBus(accountEventHandler);
        accountRepository = new InMemoryAccountRepository(new HashMap<>(), domainBus);
        CommandHandler<MakeDepositCommand> depositCommandHandler = new DepositCommandHandler(accountRepository);

        QueryHandler<GetBalanceFromAccountId, Balance> getBalanceFromAccountIdQueryHandler = new GetBalanceByIdQueryHandler(balanceRepository);

        commandBus = new CommandBus(Collections.singletonList(depositCommandHandler));
        queryBus = new QueryBus(Collections.singletonList(getBalanceFromAccountIdQueryHandler));
    }


    @Test
    public void should_make_a_deposit() throws Exception {
        Account emptyAccount = Account.empty();
        accountRepository.save(emptyAccount);

        MakeDepositCommand command = new MakeDepositCommand(emptyAccount.getAccountId(), Amount.of(1.0));
        commandBus.dispatch(command);

        Result<Balance> result = queryBus.ask(new GetBalanceFromAccountId(emptyAccount.getAccountId().getValue().toString()));
        assertThat(result.getValue()).isEqualTo(
                Balance.create(
                        AccountId.from(emptyAccount.getAccountId().getValue().toString()),
                        com.marmulasse.bank.query.account.balance.Amount.of(1.0)
                ));
    }
}
