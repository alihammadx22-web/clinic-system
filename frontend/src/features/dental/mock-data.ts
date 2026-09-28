import type { ClinicData } from "./types";

function day(offset: number) {
  const date = new Date();
  date.setDate(date.getDate() + offset);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export const createMockClinicData = (): ClinicData => ({
  doctors: [
    { id: "d1", name: "Dr. Sarah Mitchell", specialty: "General Dentistry", phone: "+1 (555) 010-1101", email: "s.mitchell@example.test", room: "Room 2", fee: 140, workingDays: [1, 2, 3, 4, 5], startTime: "08:00", endTime: "16:00", active: true },
    { id: "d2", name: "Dr. Daniel Carter", specialty: "Endodontics", phone: "+1 (555) 010-1102", email: "d.carter@example.test", room: "Room 3", fee: 180, workingDays: [1, 2, 4, 5], startTime: "09:00", endTime: "17:00", active: true },
    { id: "d3", name: "Dr. Maya Reed", specialty: "Orthodontics", phone: "+1 (555) 010-1103", email: "m.reed@example.test", room: "Room 5", fee: 160, workingDays: [1, 3, 4, 6], startTime: "10:00", endTime: "18:00", active: true },
  ],
  patients: [
    { id: "p1", name: "Olivia Bennett", phone: "+1 (555) 201-4401", age: 34, address: "45 Oak Street", medicalNotes: "Penicillin allergy", active: true, createdAt: day(-130) },
    { id: "p2", name: "Ethan Brooks", phone: "+1 (555) 201-4402", age: 40, address: "18 Pine Avenue", medicalNotes: "No known allergies", active: true, createdAt: day(-90) },
    { id: "p3", name: "Sophia Turner", phone: "+1 (555) 201-4403", age: 25, address: "72 Lake Road", medicalNotes: "Sensitive to latex", active: true, createdAt: day(-54) },
    { id: "p4", name: "Noah Williams", phone: "+1 (555) 201-4404", age: 47, address: "9 Garden Lane", medicalNotes: "Type 2 diabetes", active: true, createdAt: day(-31) },
    { id: "p5", name: "Ava Collins", phone: "+1 (555) 201-4405", age: 30, address: "201 Cedar Court", medicalNotes: "No known allergies", active: true, createdAt: day(-12) },
  ],
  appointments: [
    { id: "a1", patientId: "p1", doctorId: "d1", date: day(0), time: "08:30", duration: 30, notes: "Six-month recall", status: "Completed", createdAt: day(-14) },
    { id: "a2", patientId: "p2", doctorId: "d1", date: day(0), time: "09:30", duration: 30, notes: "", status: "Checked In", createdAt: day(-7) },
    { id: "a3", patientId: "p3", doctorId: "d2", date: day(0), time: "10:00", duration: 60, notes: "Review discomfort", status: "In Progress", createdAt: day(-9) },
    { id: "a4", patientId: "p4", doctorId: "d3", date: day(0), time: "11:30", duration: 30, notes: "First consultation", status: "Scheduled", createdAt: day(-4) },
    { id: "a5", patientId: "p5", doctorId: "d1", date: day(0), time: "14:00", duration: 30, notes: "", status: "Scheduled", createdAt: day(-3) },
    { id: "a6", patientId: "p1", doctorId: "d2", date: day(2), time: "10:00", duration: 60, notes: "", status: "Scheduled", createdAt: day(-2) },
    { id: "a7", patientId: "p2", doctorId: "d1", date: day(-28), time: "13:00", duration: 30, notes: "", status: "Completed", createdAt: day(-42) },
    { id: "a8", patientId: "p1", doctorId: "d1", date: day(-85), time: "09:00", duration: 30, notes: "", status: "Completed", createdAt: day(-92) },
  ],
  cases: [
    { id: "c1", patientId: "p1", doctorId: "d1", appointmentId: "a8", type: "Filling", notes: "Composite restoration completed. Review if sensitivity persists.", status: "Completed", createdDate: day(-85), completedDate: day(-85) },
    { id: "c2", patientId: "p3", doctorId: "d2", appointmentId: "a3", type: "Root Canal", notes: "Treatment in progress. Symptoms improving; complete obturation today.", status: "In Progress", createdDate: day(-21) },
    { id: "c3", patientId: "p4", doctorId: "d3", type: "Orthodontics", notes: "Initial alignment consultation and records review.", status: "Open", createdDate: day(-5) },
    { id: "c4", patientId: "p2", doctorId: "d1", appointmentId: "a7", type: "Cleaning", notes: "Routine cleaning completed; reinforce flossing.", status: "Completed", createdDate: day(-28), completedDate: day(-28) },
  ],
  exceptions: [
    { id: "e1", doctorId: "d3", date: day(1), type: "Late Arrival", startTime: "12:00", note: "Morning conference" },
    { id: "e2", doctorId: "d2", date: day(5), type: "Absence", note: "Personal leave" },
  ],
});
