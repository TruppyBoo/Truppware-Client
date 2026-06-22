package trupp.ware.interfaces;

/**
 * Exposes MultiPlayerGameMode's private {@code ensureHasSentCarriedItem()} so modules can flush a
 * pending held-slot change as its OWN packet on a quiet tick — instead of letting the next attack/use
 * flush it and bundle a slot change into an attack (Grim PacketOrderE).
 */
public interface IMultiPlayerGameModeExt {
    void truppware$ensureCarriedItem();
}
