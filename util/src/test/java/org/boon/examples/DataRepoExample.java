/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */

package org.boon.examples;

import org.boon.Str;
import org.boon.core.Function;
import org.boon.datarepo.Repo;
import org.boon.datarepo.Repos;
import org.boon.primitive.Int;
import org.boon.template.BoonTemplate;
import org.boon.template.Template;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.boon.Boon.*;
import static org.boon.Lists.lazyAdd;
import static org.boon.Lists.list;
import static org.boon.Maps.map;
import static org.boon.core.reflection.BeanUtils.atIndex;
import static org.boon.core.reflection.BeanUtils.idxList;
import static org.boon.criteria.ObjectFilter.eq;
import static org.boon.criteria.ObjectFilter.gt;
import static org.boon.criteria.Selector.*;
import static org.boon.template.BoonTemplate.jstl;

public class DataRepoExample {

    static boolean ok;


    public static void main(String... args) {

        List<Employee> employees;

        Repo<Integer,Employee> employeeRepo;


        /** Get all employees in every department. */
        employees =  idxList(Employee.class, departmentsList, "employees");


        /** It builds indexes on properties. */
        employeeRepo = Repos.builder()
                .primaryKey("id")
                .searchIndex("department.name")
                .searchIndex("salary")
                .build(int.class, Employee.class).init(employees);

        List<Employee> results =
                employeeRepo.query(eq("department.name", "HR"));

        /* Verify. */
        Int.equalsOrDie(4, results.size());

        Str.equalsOrDie("HR", results.get(0).getDepartment().getName());


        results = employeeRepo.query( eq("department.name", "HR"),
                gt("salary", 301));

        /* Verify. */
        Int.equalsOrDie(1, results.size());

        Str.equalsOrDie("HR", results.get(0).getDepartment().getName());

        Str.equalsOrDie("Sue", results.get(0).getFirstName());




        List<Map<String, Object>> employeeMaps;

        Repo<Integer,Map<String, Object>> employeeMapRepo;


        /** Get all employees in every department. */
        employeeMaps = (List<Map<String, Object>>)  idxList( departmentObjects, "employees");


        /** It builds indexes on properties. */
        employeeMapRepo = (Repo<Integer,Map<String, Object>>) (Object)
                Repos.builder()
                        .primaryKey("id")
                        .searchIndex("departmentName")
                        .searchIndex("salary")
                        .build(int.class, Map.class).init((List)employeeMaps);


        List<Map<String, Object>> resultMaps =
                employeeMapRepo.query(eq("departmentName", "HR"));

        /* Verify. */
        Int.equalsOrDie(4, resultMaps.size());

        Str.equalsOrDie("HR", (String) resultMaps.get(0).get("departmentName"));


        resultMaps = employeeMapRepo.query( eq("departmentName", "HR"),
                gt("salary", 301));

         /* Verify. */
        Int.equalsOrDie(1, resultMaps.size());

        Str.equalsOrDie("HR", (String) resultMaps.get(0).get("departmentName"));

        Str.equalsOrDie("Sue", (String) resultMaps.get(0).get("firstName"));




        /** Now with JSON. */

        String json = toJson(departmentObjects);
        puts(json);

        List<?> array =  (List<?>) fromJson(json);
        employeeMaps =
                (List<Map<String, Object>>) idxList(array, "employees");

        employeeMapRepo = (Repo<Integer,Map<String, Object>>) (Object)
                Repos.builder()
                        .primaryKey("id")
                        .searchIndex("departmentName")
                        .searchIndex("salary")
                        .build(int.class, Map.class).init((List)employeeMaps);


        resultMaps = employeeMapRepo.query(
                eq("departmentName", "HR"), gt("salary", 301));



        /* Verify. */
        Int.equalsOrDie(1, resultMaps.size());

        Str.equalsOrDie("HR", (String) resultMaps.get(0).get("departmentName"));

        Str.equalsOrDie("Sue", (String) resultMaps.get(0).get("firstName"));


        List<Map<String, Object>> list = employeeMapRepo.query(
                selects(
                        selectAs("firstName", "fn"),
                        selectAs("lastName", "ln"),
                        selectAs("contactInfo.phoneNumbers[0]", "ph"),

                        selectAs("salary", "pay", new Function<Integer, Float>() {
                            @Override
                            public Float apply(Integer salary) {
                                float pay = salary.floatValue() / 100;
                                return pay;
                            }
                        })
                )
        );

        puts (toJson(list));




        Template template = BoonTemplate.template();
        template.addFunctions(DecryptionService.class);

        template.addFunctions(Salary.class);



        list = employeeMapRepo.query(
                selects(
                        selectAsTemplate("fullName", "{{firstName}} {{lastName}}",
                                template),
                        selectAs("contactInfo.phoneNumbers[0]", "ph"),
                        selectAsTemplate("pay", "{{pay(salary)}}", template),
                        selectAsTemplate("id", "{{DecryptionService.decrypt(id)}}", template)

                )
        );

        puts (list);


        template = jstl();
        template.addFunctions(DecryptionService.class);
        template.addFunctions(Salary.class);



        list = employeeMapRepo.query(
                selects(
                        selectAsTemplate("fullName", "${lastName},${firstName}",
                                template),
                        selectAsTemplate("ph", "${contactInfo.phoneNumbers[0]}",
                                template),
                        selectAsTemplate("pay", "${pay(salary)}", template),
                        selectAsTemplate("id", "${DecryptionService.decrypt(id)}", template)

                )
        );

        puts (list);


        template = jstl();
        template.addFunctions(DecryptionService.class);
        template.addFunctions(Salary.class);



        list = employeeMapRepo.query(
                selects(
                        selectAsTemplate("fullName", "${lastName},${firstName}",
                                template),
                        selectAsTemplate("ph", "${contactInfo.phoneNumbers[0]}",
                                template),
                        selectAsTemplate("pay", "${pay(salary)}", template),
                        selectAsTemplate("id", "${DecryptionService.decrypt(id)}", template)

                ), gt("salary", 50), eq("firstName", "Rick")
        );


        puts (list);

    }

    public static class DecryptionService {
        public static String decrypt(Integer id) {
            return "" + ("" + (id == null ? "null" :  id.hashCode())).hashCode();
        }
    }

    public static class Salary {
        public static Float pay(Integer salary) {
            float pay = salary.floatValue() / 100;
            return pay;
        }

    }


    @Test
    public void test() {
        DataRepoExample.main();

    }

    public static class ContactInfo {
        String address;
        List<String> phoneNumbers;


    }

    public static class Employee implements Comparable<Employee> {
        int id;
        int salary;
        String firstName;
        String lastName;

        ContactInfo contactInfo = new ContactInfo();
        Department department;

        public Employee() {
        }

        public Employee(int id, int salary, String firstName, String lastName,
                        String... phoneNumbers) {
            this.id = id;
            this.salary = salary;
            this.firstName = firstName;
            this.lastName = lastName;

            for (String phone : phoneNumbers) {
                contactInfo.phoneNumbers = lazyAdd(contactInfo.phoneNumbers, phone);
            }
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getSalary() {
            return salary;
        }

        public void setSalary(int salary) {
            this.salary = salary;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public Department getDepartment() {
            return department;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Employee employee = (Employee) o;

            if (id != employee.id) return false;
            if (salary != employee.salary) return false;
            if (firstName != null ? !firstName.equals(employee.firstName) : employee.firstName != null) return false;
            if (lastName != null ? !lastName.equals(employee.lastName) : employee.lastName != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + salary;
            result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
            result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Employee{" +
                    "id=" + id +
                    ", salary=" + salary +
                    ", department=" + (department == null ? "NONE" : department.getName()) +
                    ", phone number=" + atIndex(this, "contactInfo.phoneNumbers[0]") +
                    ", firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    "}";
        }

        @Override
        public int compareTo(Employee otherEmployee) {
            return this.firstName.compareTo(otherEmployee.firstName);
        }

        public void setDepartment(Department department) {
            this.department =  department;
        }
    }

    public static class Department {
        private String name;

        private List<Employee> employees;

        public Department() {
        }

        public Department(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Department add(Employee... employees) {

            for (Employee employee : employees) {
                employee.setDepartment(this);
            }
            this.employees = lazyAdd(this.employees, employees);
            return this;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Department that = (Department) o;

            if (employees != null ? !employees.equals(that.employees) : that.employees != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (employees != null ? employees.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Department{" +
                    "name='" + name + '\'' +
                    ", employees=" + atIndex(employees, "id") +
                    '}';
        }
    }


    static List<Department> departmentsList = list(
            new Department("Engineering").add(
                    new Employee(1, 100, "Rick", "Hightower", "555-555-1000"),
                    new Employee(2, 200, "John", "Smith", "555-555-1215", "555-555-1214", "555-555-1213"),
                    new Employee(3, 300, "Drew", "Donaldson", "555-555-1216"),
                    new Employee(4, 400, "Nick", "LaySacky", "555-555-1217")

            ),
            new Department("HR").add(
                    new Employee(5, 100, "Dianna", "Hightower", "555-555-1218"),
                    new Employee(6, 200, "Derek", "Smith", "555-555-1219"),
                    new Employee(7, 300, "Tonya", "Donaldson", "555-555-1220"),
                    new Employee(8, 400, "Sue", "LaySacky", "555-555-9999")

            ), new Department("Manufacturing").add(),
            new Department("Sales").add(),
            new Department("Marketing").add()

    );

    static List<?> departmentObjects = list(
            map("name", "Engineering",
                    "employees", list(
                    map("id", 1, "salary", 100, "firstName", "Rick", "lastName", "Hightower",
                            "contactInfo", map("phoneNumbers",
                            list("555-555-0000")
                    )
                    ),
                    map("id", 2, "salary", 200, "firstName", "John", "lastName", "Smith",
                            "contactInfo", map("phoneNumbers", list("555-555-1215",
                            "555-555-1214", "555-555-1213"))),
                    map("id", 3, "salary", 300, "firstName", "Drew", "lastName", "Donaldson",
                            "contactInfo", map("phoneNumbers", list("555-555-1216"))),
                    map("id", 4, "salary", 400, "firstName", "Nick", "lastName", "LaySacky",
                            "contactInfo", map("phoneNumbers", list("555-555-1217")))

            )
            ),
            map("name", "HR",
                    "employees", list(
                    map("id", 5, "salary", 100, "departmentName", "HR",
                            "firstName", "Dianna", "lastName", "Hightower",
                            "contactInfo",
                            map("phoneNumbers", list("555-555-1218"))),
                    map("id", 6, "salary", 200, "departmentName", "HR",
                            "firstName", "Derek", "lastName", "Smith",
                            "contactInfo",
                            map("phoneNumbers", list("555-555-1219"))),
                    map("id", 7, "salary", 300, "departmentName", "HR",
                            "firstName", "Tonya", "lastName", "Donaldson",
                            "contactInfo", map("phoneNumbers", list("555-555-1220"))),
                    map("id", 8, "salary", 400, "departmentName", "HR",
                            "firstName", "Sue", "lastName", "LaySacky",
                            "contactInfo", map("phoneNumbers", list("555-555-9999")))

            )
            ),
            map("name", "Manufacturing", "employees", Collections.EMPTY_LIST),
            map("name", "Sales", "employees", Collections.EMPTY_LIST),
            map("name", "Marketing", "employees", Collections.EMPTY_LIST)
    );



}



