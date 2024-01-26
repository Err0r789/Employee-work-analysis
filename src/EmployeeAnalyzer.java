import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeAnalyzer {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
    public static void main(String[] args) {
        // Assuming the data is stored in a CSV file named 'employee_data.csv'
        String filePath = "src//Assignment_Timecard.csv";
        List<Map<String, String>> data = readCSV(filePath);
        analyzeData(data);
    }

    private static List<Map<String, String>> readCSV(String filePath) {
        List<Map<String, String>> data = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            String[] headers = null;

            int lineCounter = 0;

            while ((line = br.readLine()) != null) {
                lineCounter++;
                String[] values = line.split(",");
                Map<String, String> entry = new HashMap<>();

                if (headers == null) {
                    // Assuming the first line contains headers
                    headers = values;
                } else {
                    for (int i = 0; i < headers.length; i++) {
                        if (i < values.length) {
                            String value = values[i].trim();

                            if (headers[i].equals("Time") || headers[i].equals("Time Out")) {
                                if (!value.isEmpty()) {
                                    try {
                                        Date parsedDate = dateFormat.parse(value);
                                        entry.put(headers[i], dateFormat.format(parsedDate)); // Store the parsed and formatted date
                                    } catch (ParseException e) {
                                        System.err.println("Error parsing date in row " + lineCounter + ": " + e.getMessage());
                                    }
                                } else {
                                    entry.put(headers[i], ""); // Empty date value
                                }
                            } else {
                                entry.put(headers[i], value);
                            }

                        }
                    }
                    data.add(entry);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    private static void analyzeData(List<Map<String, String>> data) {
        int consecutiveDaysThreshold = 7;
        long timeBetweenShiftsLowerLimit = 60 * 60 * 1000; // 1 hour in milliseconds
        long timeBetweenShiftsUpperLimit = 10 * 60 * 60 * 1000; // 10 hours in milliseconds
        long singleShiftDurationLimit = 14 * 60 * 60 * 1000; // 14 hours in milliseconds

        Map<String, Map<String, List<Date>>> employeeMap = new HashMap<>();

        for (Map<String, String> entry : data) {
            String positionId = entry.get("Position ID");
            String employeeName = entry.get("Employee Name");
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a");

            try {
                Date timeIn = dateFormat.parse(entry.get("Time"));
                Date timeOut = dateFormat.parse(entry.get("Time Out"));

                if (!employeeMap.containsKey(positionId)) {
                    employeeMap.put(positionId, new HashMap<>());
                }

                if (!employeeMap.get(positionId).containsKey(employeeName)) {
                    employeeMap.get(positionId).put(employeeName, new ArrayList<>());
                }

                employeeMap.get(positionId).get(employeeName).add(timeIn);
                employeeMap.get(positionId).get(employeeName).add(timeOut);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<String, Map<String, List<Date>>> entry : employeeMap.entrySet()) {
            String positionId = entry.getKey();
            Map<String, List<Date>> employeeData = entry.getValue();

            for (Map.Entry<String, List<Date>> employeeEntry : employeeData.entrySet()) {
                String employeeName = employeeEntry.getKey();
                List<Date> shifts = employeeEntry.getValue();

                // Check for employees who worked for 7 consecutive days
                for (int i = 0; i <= shifts.size() - (2 * consecutiveDaysThreshold); i += 2) {
                    List<Date> consecutiveDays = new ArrayList<>();
                    for (int j = 0; j < consecutiveDaysThreshold * 2; j += 2) {
                        consecutiveDays.add(shifts.get(i + j));
                    }
                    if (areConsecutiveDays(consecutiveDays)) {
                        System.out.println(employeeName + " (Position ID: " + positionId + ") worked for 7 consecutive days.");
                        break;
                    }
                }

                // Check for employees with less than 10 hours between shifts but more than 1 hour
                for (int i = 0; i < shifts.size() - 2; i += 2) {
                    long timeBetweenShifts = shifts.get(i + 2).getTime() - shifts.get(i + 1).getTime();
                    if (timeBetweenShifts < timeBetweenShiftsUpperLimit && timeBetweenShifts > timeBetweenShiftsLowerLimit) {
                        System.out.println(employeeName + " (Position ID: " + positionId + ") has less than 10 hours but more than 1 hour between shifts.");
                        break;
                    }
                }

                // Check for employees who worked for more than 14 hours in a single shift
                for (int i = 0; i < shifts.size(); i += 2) {
                    long shiftDuration = shifts.get(i + 1).getTime() - shifts.get(i).getTime();
                    if (shiftDuration > singleShiftDurationLimit) {
                        System.out.println(employeeName + " (Position ID: " + positionId + ") worked for more than 14 hours in a single shift.");
                        break;
                    }
                }
            }
        }
    }

    private static boolean areConsecutiveDays(List<Date> dates) {
        for (int i = 0; i < dates.size() - 1; i++) {
            if (dates.get(i + 1).getTime() - dates.get(i).getTime() != 24 * 60 * 60 * 1000) {
                return false;
            }
        }
        return true;
    }
}
