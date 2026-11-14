import { todayIsoDate } from "@/lib/dates";
import { pictureUrl } from "@/lib/pictures";
import { formatPhoneNumber, formatPostalCode } from "@/utils/formatters";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE = /^(\+\d{3} )?\d{3} \d{3} \d{3}$/;
const POSTAL_CODE = /^\d{3} \d{2}$/;
const PHONE_MESSAGE = "Telefon ve tvaru 123 456 789 nebo +420 123 456 789";
const GUARDIAN_MESSAGE = "U nesvéprávného klienta vyplňte jméno a kontakt opatrovníka";

export const OLDEST_BIRTH_DATE = "1900-01-01";
export const SUPPORTED_HOUSING = "SUPPORTED_HOUSING";

export function emptyClient(defaultDepartmentId) {
    return toFormValues({ department: defaultDepartmentId ? { id: defaultDepartmentId } : null });
}

export function toFormValues(client) {
    return {
        picture: client?.id ? pictureUrl("clients", client.id, client.pictureId, "full") : null,
        titleBefore: client?.titleBefore ?? "",
        firstName: client?.firstName ?? "",
        lastName: client?.lastName ?? "",
        titleAfter: client?.titleAfter ?? "",
        gender: client?.gender ?? "",
        dateOfBirth: client?.dateOfBirth ?? "",
        personalNumber: client?.personalNumber ?? "",
        departmentId: client?.department?.id ?? null,
        caregiverId: client?.caregiver?.id ?? null,
        street: client?.street ?? "",
        city: client?.city ?? "",
        postalCode: formatPostalCode(client?.postalCode ?? ""),
        accommodationType: client?.accommodationType ?? "",
        supportedHousingId: client?.supportedHousing?.id ?? null,
        travelDistanceKm: client?.travelDistanceKm == null ? "" : String(client.travelDistanceKm),
        hasElevator: client?.hasElevator ?? null,
        floorOrRoom: client?.floorOrRoom ?? "",
        accessMethod: client?.accessMethod ?? "",
        lockCode: client?.lockCode ?? "",
        accessNote: client?.accessNote ?? "",
        email: client?.email ?? "",
        phone: formatPhoneNumber(client?.phone ?? ""),
        phone2: formatPhoneNumber(client?.phone2 ?? ""),
        legallyCompetent: client?.legallyCompetent ?? true,
        guardianName: client?.guardianName ?? "",
        guardianContact: client?.guardianContact ?? "",
        intakeAssessmentDate: client?.intakeAssessmentDate ?? "",
        careFrom: client?.careFrom ?? "",
        careTo: client?.careTo ?? "",
        contractFrom: client?.contractFrom ?? "",
        contractIndefinite: client?.contractIndefinite ?? true,
        contractTo: client?.contractTo ?? "",
        noCharge: client?.noCharge ?? false,
        benefits: client?.benefits ?? "NONE",
        taskIds: client?.tasks?.map((task) => task.id) ?? [],
        relativesContact: client?.relativesContact ?? "",
        generalPractitioner: client?.generalPractitioner ?? "",
        notes: client?.notes ?? "",
        terminationDate: client?.terminationDate ?? "",
        terminationReason: client?.terminationReason ?? "",
    };
}

/** `withTermination` requires the contract end fields (inactive clients). */
export function validateClient(values, { withTermination = false } = {}) {
    const errors = {};
    if (!values.firstName.trim()) errors.firstName = "Prosím zadejte jméno";
    if (!values.lastName.trim()) errors.lastName = "Prosím zadejte příjmení";
    if (!values.gender) errors.gender = "Prosím vyberte pohlaví";
    if (!values.dateOfBirth) errors.dateOfBirth = "Prosím zadejte datum narození";
    if (!values.departmentId) errors.departmentId = "Prosím vyberte středisko";
    if (!values.caregiverId) errors.caregiverId = "Prosím vyberte klíčového pracovníka";
    if (values.accommodationType === SUPPORTED_HOUSING) {
        // the server copies the address from the chosen house
        if (!values.supportedHousingId) errors.supportedHousingId = "Vyberte dům s pečovatelskou službou";
    } else {
        if (!values.street.trim()) errors.street = "Prosím zadejte ulici a číslo popisné";
        if (!values.city.trim()) errors.city = "Prosím zadejte město";
        if (!values.postalCode) errors.postalCode = "Prosím zadejte PSČ";
        else if (!POSTAL_CODE.test(values.postalCode)) errors.postalCode = "PSČ ve tvaru 123 45";
    }
    if (values.travelDistanceKm.trim()) {
        if (!/^-?\d+$/.test(values.travelDistanceKm.trim())) errors.travelDistanceKm = "Zadejte dojezdovou vzdálenost v celých kilometrech";
        else if (Number(values.travelDistanceKm) < 0) errors.travelDistanceKm = "Dojezdová vzdálenost nesmí být záporná";
    }
    if (values.email.trim() && !EMAIL.test(values.email.trim())) errors.email = "Zadejte platnou e-mailovou adresu";
    if (values.phone && !PHONE.test(values.phone)) errors.phone = PHONE_MESSAGE;
    if (values.phone2 && !PHONE.test(values.phone2)) errors.phone2 = PHONE_MESSAGE;
    if (!values.legallyCompetent) {
        if (!values.guardianName.trim()) errors.guardianName = GUARDIAN_MESSAGE;
        if (!values.guardianContact.trim()) errors.guardianContact = GUARDIAN_MESSAGE;
    }
    if (!values.contractIndefinite) {
        if (!values.contractTo) errors.contractTo = "U smlouvy na dobu určitou vyplňte datum konce";
        else if (values.contractFrom && values.contractTo < values.contractFrom) errors.contractTo = "Konec smlouvy nesmí být před jejím začátkem";
    }
    if (values.careFrom && values.careTo && values.careTo < values.careFrom) errors.careTo = "Konec péče nesmí být před jejím začátkem";
    // the number goes into the 10-digit payment variable symbol after year and month
    if (values.personalNumber && !/^\d{6}$/.test(values.personalNumber)) errors.personalNumber = "Osobní číslo musí mít 6 číslic";
    if (withTermination) {
        if (!values.terminationDate) errors.terminationDate = "Prosím zadejte datum ukončení";
        else if (values.terminationDate > todayIsoDate()) errors.terminationDate = "Datum ukončení nemůže být v budoucnosti";
        if (!values.terminationReason) errors.terminationReason = "Prosím vyberte důvod";
    }
    return errors;
}

const orNull = (text) => text.trim() || null;

export function toRequest(values) {
    return {
        picture: values.picture,
        titleBefore: orNull(values.titleBefore),
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        titleAfter: orNull(values.titleAfter),
        gender: values.gender,
        dateOfBirth: values.dateOfBirth,
        personalNumber: values.personalNumber || null,
        departmentId: values.departmentId,
        caregiverId: values.caregiverId,
        street: values.street.trim(),
        city: values.city.trim(),
        postalCode: values.postalCode,
        accommodationType: values.accommodationType || null,
        supportedHousingId: values.accommodationType === SUPPORTED_HOUSING ? values.supportedHousingId : null,
        travelDistanceKm: values.travelDistanceKm.trim() ? Number(values.travelDistanceKm) : null,
        hasElevator: values.hasElevator,
        floorOrRoom: orNull(values.floorOrRoom),
        accessMethod: values.accessMethod || null,
        lockCode: orNull(values.lockCode),
        accessNote: orNull(values.accessNote),
        email: orNull(values.email),
        phone: values.phone || null,
        phone2: values.phone2 || null,
        legallyCompetent: values.legallyCompetent,
        guardianName: orNull(values.guardianName),
        guardianContact: orNull(values.guardianContact),
        intakeAssessmentDate: values.intakeAssessmentDate || null,
        careFrom: values.careFrom || null,
        careTo: values.careTo || null,
        contractFrom: values.contractFrom || null,
        contractIndefinite: values.contractIndefinite,
        contractTo: values.contractIndefinite ? null : values.contractTo || null,
        noCharge: values.noCharge,
        benefits: values.benefits,
        taskIds: values.taskIds,
        relativesContact: orNull(values.relativesContact),
        generalPractitioner: orNull(values.generalPractitioner),
        notes: orNull(values.notes),
        terminationDate: values.terminationDate || null,
        terminationReason: values.terminationReason || null,
    };
}
