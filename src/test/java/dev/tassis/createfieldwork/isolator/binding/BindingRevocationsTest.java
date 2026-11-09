package dev.tassis.createfieldwork.isolator.binding;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BindingRevocationsTest {
    @Test void issuedIdIsNeverATargetAndUnknownDenies() {
        var ledger = new BindingRevocations();
        assertFalse(ledger.permits(UUID.randomUUID(), 1));
        var id = ledger.issue();
        assertTrue(ledger.permits(id, 1));
        assertFalse(ledger.permits(id, 0));
        assertFalse(ledger.permits(id, 2));
    }
    @Test void revokePersistsAndCannotResurrect() {
        var ledger = new BindingRevocations(); var id = ledger.issue();
        ledger.revoke(id); long tombstone = ledger.generation(id); ledger.revoke(id);
        assertEquals(tombstone, ledger.generation(id));
        assertFalse(ledger.permits(id, 1));
        var loaded = BindingRevocations.load(ledger.save(new CompoundTag(), null), null);
        assertTrue(loaded.revoked(id)); assertFalse(loaded.permits(id, 1));
        assertNotEquals(id, loaded.issue());
    }
    @Test void missingSchemaAndMalformedDataFailClosed() {
        var bad = BindingRevocations.load(new CompoundTag(), null);
        assertFalse(bad.healthy()); assertFalse(bad.permits(UUID.randomUUID(), 1));
        assertThrows(IllegalStateException.class, bad::issue);
        assertThrows(IllegalStateException.class, () -> bad.revoke(UUID.randomUUID()));
    }
    @Test void serverLedgersDoNotShareState() {
        var one = new BindingRevocations(); var two = new BindingRevocations();
        var id = one.issue(); assertFalse(two.permits(id, 1));
        two.revoke(id); assertTrue(one.permits(id, 1));
    }
}
