type Id = int;
type Owner = int;
type DeepId = Id;
type DeeperId = DeepId;

fun int.nearest(self): int1 { return 0; }
fun Id.nearest(self): int2 { return 0; }
fun DeepId.nearest(self): int3 { return 0; }
fun int.preferGeneric(self): int4 { return 0; }
fun T.preferGeneric(self): int5 { return 0; }

fun checkAliases(value: DeeperId, owner: Owner) {
    val id: Id = owner;
    val copy: Owner = id;
    __expect_type(id, "Id");
    __expect_type(copy, "Owner");
    __expect_type(value.nearest(), "int3");
    __expect_type(value.preferGeneric(), "int5");
    __expect_type((0 as int8).nearest(), "int1");
}

struct Box<T> { value: T }
fun Box<int>.boxKind(self): int1 { return 0; }
fun Box<Id>.boxKind(self): int2 { return 0; }
fun Box<T>.boxKind(self): int3 { return 0; }
fun checkContainers(value: Box<DeepId>) {
    val widened: Box<int> = value;
    __expect_type(widened, "Box<int>");
    __expect_type(widened.boxKind(), "int1");
    __expect_type(value.boxKind(), "int3");
}

fun array<int>.arrayKind(self): int1 { return 0; }
fun array<Id>.arrayKind(self): int2 { return 0; }
fun checkArray(value: array<DeepId>) {
    __expect_type(value.arrayKind(), "int2");
}

fun (int, int).tensorKind(self): int1 { return 0; }
fun (Id, int).tensorKind(self): int2 { return 0; }
fun [int, int].tupleKind(self): int1 { return 0; }
fun [Id, int].tupleKind(self): int2 { return 0; }
fun checkShapes(tensor: (DeepId, int), tuple: [DeepId, int]) {
    __expect_type(tensor.tensorKind(), "int2");
    __expect_type(tuple.tupleKind(), "int2");
}

fun (int | slice).unionKind(self): int1 { return 0; }
fun (Id | slice).unionKind(self): int2 { return 0; }
fun checkUnion(value: slice | DeepId) {
    __expect_type(value.unionKind(), "int2");
    if (value is Id) {
        __expect_type(value, "Id");
    }
    match (value) {
        int => {
            __expect_type(value, "int");
        }
        slice => {}
    }
}

type Tagged<T> = cell;
type OtherTag<T> = cell;
fun Tagged<T>.tagKind(self): int1 { return 0; }
fun OtherTag<T>.tagKind(self): int2 { return 0; }
fun Tagged<Id>.tagDistance(self): int3 { return 0; }
fun Tagged<int>.tagDistance(self): int4 { return 0; }
fun checkTags(value: Tagged<DeepId>, other: OtherTag<int>) {
    val assigned: OtherTag<int> = value;
    __expect_type(assigned, "OtherTag<int>");
    __expect_type(value.tagKind(), "int1");
    __expect_type(other.tagKind(), "int2");
    __expect_type(value.tagDistance(), "int3");
}

type Balance = cell?;
type Assets = cell?;
fun Balance.validate(self): int1 { return 0; }
fun Assets.validate(self): int2 { return 0; }
struct Holder<T> { padding: int; data: T }
fun checkDeclaredReceiver(value: Balance, holder: Holder<Balance>, tuple: (int, Balance)) {
    if (value == null || holder.data == null || tuple.1 == null) { return; }
    __expect_type(value, "cell");
    __expect_type(value.validate(), "int1");
    __expect_type(holder.data.validate(), "int1");
    __expect_type(tuple.1.validate(), "int1");
}

fun (int -> int).callbackKind(self): int1 { return 0; }
fun (Id -> int).callbackKind(self): int2 { return 0; }
type Key = uint32;
type DeepKey = Key;
fun map<uint32, uint32>.mapKind(self): int1 { return 0; }
fun map<Key, uint32>.mapKind(self): int2 { return 0; }
fun checkCallableAndMap(callback: DeepId -> int, table: map<DeepKey, uint32>) {
    __expect_type(callback.callbackKind(), "int2");
    __expect_type(table.mapKind(), "int2");
}

fun cell.retain(self): self { return self; }
fun cell.retainAs<T>(self): self { return self; }
fun cell.preferNarrowed(self): int3 { return 0; }
fun Balance.preferNarrowed(self): int4 { return 0; }
fun checkMethodChain(value: Balance) {
    if (value == null) { return; }
    __expect_type(value.retain().validate(), "int1");
    __expect_type(value.retainAs<int>().validate(), "int1");
    __expect_type(value.preferNarrowed(), "int3");
}

fun main() {}
