get/*<# 0x11901 #>*/ fun initialId(): int {
    val storage = lazy Storage.load();
    return storage.id;
}
