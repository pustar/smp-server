package top.itning.smp.smpinfo.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itning.smp.smpinfo.dao.ApartmentDao;
import top.itning.smp.smpinfo.dao.StudentUserDao;
import top.itning.smp.smpinfo.dto.ApartmentDTO;
import top.itning.smp.smpinfo.entity.Apartment;
import top.itning.smp.smpinfo.exception.NotInLineWithBusinessLogicException;
import top.itning.smp.smpinfo.exception.NullFiledException;
import top.itning.smp.smpinfo.service.ApartmentService;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author itning
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ApartmentServiceImpl implements ApartmentService {
    private final ApartmentDao apartmentDao;

    private final StudentUserDao studentUserDao;

    public ApartmentServiceImpl(ApartmentDao apartmentDao, StudentUserDao studentUserDao) {
        this.apartmentDao = apartmentDao;
        this.studentUserDao = studentUserDao;
    }

    @Override
    public List<Apartment> getAllApartments() {
        return apartmentDao.findAll();
    }

    @Override
    public void updateApartment(Apartment apartment) {
        if (apartment == null || StringUtils.isBlank(apartment.getId())) {
            throw new NullFiledException("ID为空", HttpStatus.BAD_REQUEST);
        }
        if (apartmentDao.findByName(apartment.getName()) != null) {
            throw new NullFiledException("公寓名不能重复", HttpStatus.BAD_REQUEST);
        }
        Apartment a = apartmentDao.findById(apartment.getId()).orElseThrow(() -> new NullFiledException("公寓不存在", HttpStatus.BAD_REQUEST));
        a.setName(apartment.getName());
        apartmentDao.save(a);
    }

    @Override
    public void delApartment(String id) {
        if (StringUtils.isBlank(id)) {
            throw new NullFiledException("ID为空", HttpStatus.BAD_REQUEST);
        }
        long count = studentUserDao.countByApartmentId(id);
        if (count != 0L) {
            throw new NotInLineWithBusinessLogicException("该公寓还有" + count + "人，不能删除", HttpStatus.BAD_REQUEST);
        }
        apartmentDao.deleteById(id);
    }

    @Override
    public Apartment saveApartment(String name) {
        if (StringUtils.isBlank(name)) {
            throw new NullFiledException("公寓名不能为空", HttpStatus.BAD_REQUEST);
        }
        if (apartmentDao.findByName(name) != null) {
            throw new NullFiledException("公寓名不能重复", HttpStatus.BAD_REQUEST);
        }
        Apartment apartment = new Apartment();
        apartment.setName(name);
        Date date = new Date();
        apartment.setGmtCreate(date);
        apartment.setGmtModified(date);
        return apartmentDao.save(apartment);
    }

    @Override
    public List<ApartmentDTO> getAllApartmentsWithPeople() {
        return apartmentDao.findAll()
                .parallelStream()
                .map(apartment -> {
                    ApartmentDTO apartmentDTO = new ApartmentDTO();
                    apartmentDTO.setName(apartment.getName());
                    apartmentDTO.setPeople(studentUserDao.countByApartmentId(apartment.getId()));
                    return apartmentDTO;
                })
                .collect(Collectors.toList());
    }
}
