import "@stdlib/tvm-dicts"

fun cell?.addIntToIDict(mutate self, key: int, number: int, b: builder): void {
    return self.iDictSetBuilder(32, key, b);
}
