package ec.ecu.ups.icc.employees.service;

import ec.ecu.ups.icc.employees.dto.*;
import ec.ecu.ups.icc.employees.entity.Company;
import ec.ecu.ups.icc.employees.entity.Department;
import ec.ecu.ups.icc.employees.entity.Employee;
import ec.ecu.ups.icc.employees.exception.ConflictException;
import ec.ecu.ups.icc.employees.exception.ResourceNotFoundException;
import ec.ecu.ups.icc.employees.repository.CompanyRepository;
import ec.ecu.ups.icc.employees.repository.DepartmentRepository;
import ec.ecu.ups.icc.employees.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EmployeeService {
    
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    
    public EmployeeService(DepartmentRepository departmentRepository, 
                          EmployeeRepository employeeRepository,
                          CompanyRepository companyRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
    }
    
   
    public DepartmentWithEmployeesDto getDepartmentWithEmployees(Long departmentId, String sort) {
        
        Department department = departmentRepository.findByIdAndActive(departmentId, "S")
                .orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado o inactivo"));
        
        
        List<Employee> employees;
        if ("asc".equalsIgnoreCase(sort)) {
            employees = employeeRepository.findActiveByDepartmentIdOrderBySalaryAsc(departmentId);
        } else {
            employees = employeeRepository.findActiveByDepartmentIdOrderBySalaryDesc(departmentId);
        }
        
        
        List<EmployeeDto> employeeDtos = employees.stream()
                .map(e -> new EmployeeDto(e.getId(), e.getFirstName(), e.getLastName(), 
                                         e.getEmail(), e.getSalary()))
                .collect(Collectors.toList());
        
        
        BigDecimal totalSalaries = employees.stream()
                .map(Employee::getSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        
        Company company = department.getCompany();
        CompanyDto companyDto = new CompanyDto(company.getId(), company.getName(), company.getCountry());
        
        
        return new DepartmentWithEmployeesDto(
                department.getId(),
                department.getName(),
                department.getBudget(),
                companyDto,
                employeeDtos,
                employeeDtos.size(),
                totalSalaries
        );
    }
    
    public CompanyDepartmentsDto getCompanyDepartments(Long companyId) {
       
        Company company = companyRepository.findByIdAndActive(companyId, "S")
                .orElseThrow(() -> new ResourceNotFoundException("Compania no encontrado o inactiva"));
        
       
        List<Department> departments = departmentRepository.findActiveByCompanyId(companyId);
        
       
        List<DepartmentSummaryDto> departmentDtos = new ArrayList<>();
        BigDecimal totalBudget = BigDecimal.ZERO;
        
        for (Department dept : departments) {
            long employeeCount = employeeRepository.countActiveByDepartmentId(dept.getId());
            departmentDtos.add(new DepartmentSummaryDto(
                    dept.getId(),
                    dept.getName(),
                    dept.getBudget(),
                    (int) employeeCount
            ));
            totalBudget = totalBudget.add(dept.getBudget());
        }
        
       
        return new CompanyDepartmentsDto(
                company.getId(),
                company.getName(),
                company.getCountry(),
                departmentDtos.size(),
                departmentDtos,
                totalBudget
        );
    }
    
   
    public EmployeesResponseDto getHighSalaryEmployees(Long companyId, Double minSalary) {
      
        Company company = companyRepository.findByIdAndActive(companyId, "S")
                .orElseThrow(() -> new ResourceNotFoundException("Compania no encontrada o inactiva"));
        
        
        BigDecimal minSalaryBd = BigDecimal.valueOf(minSalary);
        List<Employee> employees = employeeRepository.findActiveByCompanyIdAndMinSalary(companyId, minSalaryBd);
        
    
        List<EmployeeWithDepartmentDto> employeeDtos = employees.stream()
                .map(e -> {
                    DepartmentInfoDto deptDto = new DepartmentInfoDto(
                            e.getDepartment().getId(),
                            e.getDepartment().getName()
                    );
                    return new EmployeeWithDepartmentDto(
                            e.getId(),
                            e.getFirstName(),
                            e.getLastName(),
                            e.getEmail(),
                            e.getSalary(),
                            deptDto
                    );
                })
                .collect(Collectors.toList());
        
        
        return new EmployeesResponseDto(
                company.getName(),
                minSalary,
                employeeDtos.size(),
                employeeDtos
        );
    }
    
    /**
     * ENDPOINT 4: Transfer employee to another department
     */
    @Transactional
    public EmployeeTransferResponseDto transferEmployee(Long employeeId, Long newDepartmentId) {
        // Find employee and verify it's active
        Employee employee = employeeRepository.findByIdAndActive(employeeId, "S")
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado o inactivo"));
        
        // Find target department and verify it's active
        Department newDepartment = departmentRepository.findByIdAndActive(newDepartmentId, "S")
                .orElseThrow(() -> new ResourceNotFoundException("Departamento objetivo no encontrado o inactivo"));
        
        // Get current department
        Department oldDepartment = employee.getDepartment();
        
        // Check if employee is already in the target department
        if (oldDepartment.getId().equals(newDepartmentId)) {
            throw new ConflictException("Empleado listo en el departamento objetivo");
        }
        
        // Create DTOs for old and new departments
        DepartmentInfoDto oldDeptDto = new DepartmentInfoDto(oldDepartment.getId(), oldDepartment.getName());
        DepartmentInfoDto newDeptDto = new DepartmentInfoDto(newDepartment.getId(), newDepartment.getName());
        
        // Transfer employee
        employee.setDepartment(newDepartment);
        employeeRepository.save(employee);
        
        // Create response
        String employeeName = employee.getFirstName() + " " + employee.getLastName();
        return new EmployeeTransferResponseDto(
                employee.getId(),
                employeeName,
                oldDeptDto,
                newDeptDto,
                "Empleado transferido correctamente."
        );
    }
}
