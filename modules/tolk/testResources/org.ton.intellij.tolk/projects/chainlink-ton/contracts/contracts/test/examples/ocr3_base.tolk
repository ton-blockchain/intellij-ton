import "./../../lib/ocr/multi_ocr3_base.tolk";
import "./../../lib/ocr/types.tolk";
import "./../../lib/ocr/exit_codes.tolk";
import "./../../lib/utils.tolk";

struct OCR3BaseExampleContract {
    contractId: uint64;
    ocr3Base: OCR3Base;
}

fun OCR3BaseExampleContract.load() {
    return OCR3BaseExampleContract.fromCell(contract.getData());
}

fun OCR3BaseExampleContract.store(self) {
    contract.setData(self.toCell());
}

struct (0x00000002) Transmit {
    ocrPluginType: uint16,
    reportContext: ReportContext,
    report: cell,
    signatures: cell, //Vector<SignatureEd25519>
}

// Support the SetOCR3Config message from multi_ocr3_base.tolk
type IncomingMessage = Transmit | OCR3Base_SetOCR3Config;

fun onInternalMessage(in: InMessage) {
    val msg = lazy IncomingMessage.fromSlice(in.body);

    match (msg) {
        Transmit => {
        //Using the transmit function validates signatures and emits an event
            var storage = lazy OCR3BaseExampleContract.load();
            val sender = in.senderAddress;
            storage.ocr3Base.transmit(
                sender,
                msg.ocrPluginType,
                msg.reportContext,
                msg.report,
                msg.signatures
            );
        },
        //The SetOCR3Config message configures authorized signers and transmitters
        OCR3Base_SetOCR3Config => {
            // On a real contract you would invoke requireOwner here
            var storage = lazy OCR3BaseExampleContract.load();
            storage.ocr3Base.setOcr3Config(msg);
            storage.store();
        },
        else => {
            // ignore empty messages, "wrong opcode" for others
            assert (in.body.isEmpty()) throw 0xFFFF
        }        
     }
}

get fun ocr3ConfigInfo(ocrPluginType: uint16): ConfigInfo {
    val s = lazy OCR3BaseExampleContract.load();
    val config = s.ocr3Base.getConfig(ocrPluginType);
    assert(config != null, ERROR_NON_EXISTENT_OCR_PLUGIN_TYPE);
    return config!.configInfo;
}

get fun ocr3Signers(ocrPluginType: uint16): tuple? {
    val s = lazy OCR3BaseExampleContract.load();
    val config = s.ocr3Base.getConfig(ocrPluginType);
    assert(config != null, ERROR_NON_EXISTENT_OCR_PLUGIN_TYPE);
    var d = config!.signers;
    return d.keysDestructs(); 
}

get fun ocr3Transmitters(ocrPluginType: uint16): tuple? {
    val s = lazy OCR3BaseExampleContract.load();
    val config = s.ocr3Base.getConfig(ocrPluginType);
    assert(config != null, ERROR_NON_EXISTENT_OCR_PLUGIN_TYPE);
    var d  = config!.transmitters;
    return d.keysDestructs(); 
}

