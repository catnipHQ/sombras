package com.livrodassombras.summon;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A creature summoned with the Livro dos Espíritos that can't be a tamed animal
 * (the Vex Espiritual is a Vex, the Golem de Almas an Iron Golem). It still belongs to its summoner.
 */
public interface SpiritBound {
    @Nullable
    UUID getSummonerUUID();
}
